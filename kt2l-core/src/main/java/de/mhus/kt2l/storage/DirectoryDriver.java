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
        protected InputStream openFileStream(String path, String name) throws IOException {
            var pathAndName = MFile.normalizePath(path + "/" + name);
            final var file = new File(root, pathAndName);
            if (file.exists() && file.isFile()) {
                return new FileInputStream(file);
            }
            throw new FileNotFoundException(path);
        }

        @Override
        public OutputStream createFileStreamInternal(String path, String name) throws IOException {
            final var file = new File(root, MFile.normalizePath(path + "/" + name));
            file.getParentFile().mkdirs();
            return new FileOutputStream(file);
        }

        @Override
        public List<StorageFile> listFiles(String path) throws IOException {
            var list = new File(root, MFile.normalizePath(path)).listFiles();
            return Arrays.stream(list == null ? new File[0] : list)
                    .filter(f -> !f.getName().startsWith("."))
                    .map(f -> new StorageFile(
                            this,
                            path,
                            f.getName(),
                            f.isDirectory(),
                            f.length(),
                            f.lastModified()
                    )).toList();
        }

        @Override
        public boolean isLocal() {
            return true;
        }

        @Override
        public String getLocalPath(StorageFile path) throws IOException {
            final var file = new File(root, MFile.normalizePath(path.getPathAndName()));
            if (!file.exists()) {
                throw new FileNotFoundException(path.getPathAndName());
            }
            return file.getAbsolutePath();
        }

        @Override
        public void delete(StorageFile file) {
            final var f = new File(root, MFile.normalizePath(file.getPathAndName()));
            if (f.exists()) {
                if (f.isFile())
                    f.delete();
                else if (f.isDirectory())
                    MFile.deleteDir(f);
            }
        }

    }
}
