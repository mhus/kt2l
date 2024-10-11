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
import io.azam.ulidj.ULID;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public abstract class Storage {

    private static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd/");

    public OutputFile createFileStream(StorageFile path, String name) throws IOException {
        // TODO validate is a storage place
        name = MFile.normalize(name);
        return new OutputFile(this, path.getPathAndName() + "/" + name, name, false, 0, System.currentTimeMillis(), createFileStreamInternal(path.getPathAndName(), name));
    }

    public OutputFile createFileStream(String context, String name) throws IOException {
        final var path = createFilePath(context);
        name = MFile.normalize(name);
        return new OutputFile(this, path, name, false, 0, System.currentTimeMillis(), createFileStreamInternal(path, name));
    }

    public InputFile openFile(String pathAndName) throws IOException {
        return new InputFile(this, MFile.getParentPath(pathAndName), MFile.getFileName(pathAndName), false, 0, 0, openFileStream(MFile.getParentPath(pathAndName), MFile.getFileName(pathAndName)));
    }

    /**
     *
     * @param path
     * @return
     * @throws IOException, FileNotFoundException if file was not found.
     */
    protected abstract InputStream openFileStream(String path, String name) throws IOException;

    private String createFilePath(String context) {
        return dateFormat.format(new Date()) + ULID.random() + "_" + MFile.normalize(context);
    }

    protected abstract OutputStream createFileStreamInternal(String path, String name) throws IOException;

    protected OutputStream createFileStream(StorageFile storageFile) throws IOException {
        return createFileStreamInternal(storageFile.getPath(), storageFile.getName());
    }

    /**
     *
     * @param path
     * @return
     * @throws IOException, FileNotFoundException if directory was not found.
     */
    public abstract List<StorageFile> listFiles(String path) throws IOException;

    public List<StorageFile> listFiles(StorageFile path) throws IOException {
        return listFiles(path.getPath() == null ? "" : path.getPathAndName());
    }

    /**
     * Returns true if storage is a local storage system.
     * @return
     */
    public abstract boolean isLocal();

    /**
     *
     * @param path
     * @return
     * @throws IOException if storage is not local or FileNotFoundExeption if file/directory was not found.
     */
    public abstract String getLocalPath(StorageFile path) throws IOException;

    public StorageFile createDirectory(String context) throws IOException {
        final var path = createFilePath(context);
        return new StorageFile(this, path, true, 0, System.currentTimeMillis());
    }

    public abstract void delete(StorageFile file);
}
