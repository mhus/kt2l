package de.mhus.kt2l.storage;

import lombok.Getter;

import java.io.OutputStream;

@Getter
public class OutputFile extends StorageFile {
    private OutputStream stream;

    OutputFile(Storage storage, String path, String name, boolean directory, long size, OutputStream stream) {
        super(storage, path, name, directory, size);
        this.stream = stream;
    }

}
