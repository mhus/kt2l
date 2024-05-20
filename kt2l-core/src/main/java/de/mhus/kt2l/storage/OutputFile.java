package de.mhus.kt2l.storage;

import lombok.Getter;

import java.io.OutputStream;

@Getter
public class OutputFile extends StorageFile {
    private OutputStream stream;

    OutputFile(Storage storage, String path, String name, boolean directory, long size, long modified, OutputStream stream) {
        super(storage, path, name, directory, size, modified);
        this.stream = stream;
    }

}
