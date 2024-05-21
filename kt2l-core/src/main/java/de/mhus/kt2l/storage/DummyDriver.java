/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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

        @Override
        public void delete(StorageFile file) {
            // do nothing
        }
    }
}
