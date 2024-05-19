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
    private final Storage storage;

    StorageFile(Storage storage, String path, String name, boolean directory, long size) {
        this.storage = storage;
        this.path = path;
        this.name = name;
        this.directory = directory;
        this.size = size;
    }

    public StorageFile(StorageFile parent, String path) {
        this(parent.getStorage(), parent.getPath() + "/" + MFile.normalizePath(path), MString.beforeLastIndex(path, '/'), false, -1);
    }

}
