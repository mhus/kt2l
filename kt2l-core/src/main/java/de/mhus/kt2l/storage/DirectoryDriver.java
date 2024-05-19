package de.mhus.kt2l.storage;

import de.mhus.commons.tools.MFile;
import de.mhus.commons.tools.MString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

@Component
public class DirectoryDriver implements BucketDriver {

    public static final String NAME = "directory";

    @Value("${kt2l.storage.directory.home:target/storage/}")
    private String home;

    @Override
    public boolean supports(StorageConfiguration.Bucket bucket) {
        return NAME.equals(bucket.getType());
    }

    @Override
    public Storage createStorage(StorageConfiguration.Bucket bucket, String userName) {
        var path = MString.substitute(bucket.getRoot(), "username", userName,  "home", home);

        return new DirectoryStorage(path);
    }

    private static class DirectoryStorage extends Storage {
        private final File root;

        public DirectoryStorage(String root) {
            this.root = new File(root);
        }

        @Override
        protected InputStream openFileStream(String path) throws IOException {
            final var file = new File(root, MFile.normalizePath(path));
            if (file.exists() && file.isFile()) {
                return new FileInputStream(file);
            }
            throw new FileNotFoundException(path);
        }

        @Override
        public OutputStream createFileStream(String path) throws IOException {
            final var file = new File(root, MFile.normalizePath(path));
            file.getParentFile().mkdirs();
            return new FileOutputStream(file);
        }

        @Override
        public List<StorageFile> listFiles(String path) throws IOException {
            return Arrays.stream(new File(root, MFile.normalizePath(path)).listFiles())
                    .filter(f -> !f.getName().startsWith("."))
                    .map(f -> new StorageFile(
                            this,
                            path + "/" + f.getName(),
                            f.getName(),
                            f.isDirectory(),
                            f.length()
                    )).toList();
        }

        @Override
        public boolean isLocal() {
            return true;
        }

        @Override
        public String getLocalPath(StorageFile path) throws IOException {
            final var file = new File(root, MFile.normalizePath(path.getPath()));
            if (!file.exists()) {
                throw new FileNotFoundException(path.getPath());
            }
            return file.getAbsolutePath();
        }

    }
}
