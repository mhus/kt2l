package de.mhus.kt2l.fs;

import de.mhus.commons.tools.MFile;
import de.mhus.commons.tools.MString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

@Component
public class DirectoryDriver implements FileSystemDriver {

    public static final String NAME = "directory";

    @Value("${kt2l.fs.directory.home:target/fs/}")
    private String home;

    @Override
    public boolean supports(FsConfiguration.Context fsContext) {
        return NAME.equals(fsContext.getType());
    }

    @Override
    public FileSystem createFilesystem(FsConfiguration.Context fsContext, String userName) {
        var path = MString.substitute(fsContext.getRoot(), "username", userName,  "home", home);

        return new DirectoryFilesystem(path);
    }

    private static class DirectoryFilesystem implements FileSystem {
        private final File root;

        public DirectoryFilesystem(String root) {
            this.root = new File(root);
        }

        @Override
        public OutputStream createFile(String path) throws IOException {
            return new FileOutputStream(new File(root, MFile.normalizePath(path)));
        }

        @Override
        public List<FileSystemFile> listFiles(String path) throws IOException {
            return Arrays.stream(new File(root, MFile.normalizePath(path)).listFiles())
                    .filter(f -> !f.getName().startsWith("."))
                    .map(f -> new FileSystemFile(
                            path + "/" + f.getName(),
                            f.getName(),
                            f.isDirectory(),
                            f.length()
                    )).toList();
        }


    }
}
