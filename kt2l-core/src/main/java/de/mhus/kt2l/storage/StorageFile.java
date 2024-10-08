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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(of = "path")
@ToString
public class StorageFile {

    private final String path;
    private final String name;
    private final boolean directory;
    private final long size;
    private final long modified;
    private final Storage storage;

    StorageFile(Storage storage, String pathWithName, boolean directory, long size, long modified) {
        this(storage, MFile.getParentPath(pathWithName), MFile.getFileName(pathWithName), directory, size, modified);
    }
    StorageFile(Storage storage, String path, String name, boolean directory, long size, long modified) {
        this.storage = storage;
        this.path = path;
        this.name = name;
        this.directory = directory;
        this.size = size;
        this.modified = modified;
    }

    public StorageFile(StorageFile parent, String pathWithName) {
        this(parent.getStorage(), parent.getPath() + "/" + MFile.normalizePath(pathWithName), false, -1, -1);
    }

    public String getPathAndName() {
        return path + "/" + name;
    }

}
