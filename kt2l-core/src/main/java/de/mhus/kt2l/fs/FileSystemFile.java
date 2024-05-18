package de.mhus.kt2l.fs;

import de.mhus.commons.tools.MFile;

public record FileSystemFile(String path, String name, boolean directory, long size) {

    public FileSystemFile(FileSystemFile parent, String name) {
        this(parent.path() + "/" + MFile.normalizePath(name), name, false, -1);
    }

}
