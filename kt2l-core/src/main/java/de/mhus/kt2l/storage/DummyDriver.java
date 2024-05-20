package de.mhus.kt2l.storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class DummyDriver implements BucketDriver {
    @Override
    public boolean supports(StorageConfiguration.Bucket fsContext) {
        return false;
    }

    @Override
    public Storage createStorage(StorageConfiguration.Bucket fsContext, String userName) {
        return new DummyStorage();
    }

    private class DummyStorage extends Storage {
        @Override
        protected InputStream openFileStream(String path) throws IOException {
            throw new FileNotFoundException(path);
        }

        @Override
        protected OutputStream createFileStream(String path) throws IOException {
            return new OutputStream() {

                @Override
                public void write(int b) throws IOException {
                    // do nothing
                }
            };
        }

        @Override
        public List<StorageFile> listFiles(String path) throws IOException {
            if (path.equals("/")) {
                return List.of(new StorageFile(this, "/", "no_content.txt", false, 0, 0));
            }
            throw new FileNotFoundException(path);
        }

        @Override
        public boolean isLocal() {
            return false;
        }

        @Override
        public String getLocalPath(StorageFile path) throws IOException {
            throw new IOException("Not supported");
        }
    }
}
