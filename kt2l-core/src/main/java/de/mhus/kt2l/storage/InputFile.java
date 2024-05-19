package de.mhus.kt2l.storage;

import lombok.Getter;

import java.io.InputStream;

@Getter
public class InputFile extends StorageFile {

    private InputStream stream;

    InputFile(Storage storage, String path, String name, boolean directory, long size, InputStream stream) {
        super(storage, path, name, directory, size);
        this.stream = stream;
    }
}
