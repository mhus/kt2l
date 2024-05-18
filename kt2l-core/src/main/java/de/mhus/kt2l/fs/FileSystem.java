package de.mhus.kt2l.fs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface FileSystem {

    OutputStream createFile(String path) throws IOException;

    default OutputStream createFile(FileSystemFile path) throws IOException {
        return createFile(path.path());
    }

    List<FileSystemFile> listFiles(String path) throws IOException;

    default List<FileSystemFile> listFiles(FileSystemFile path) throws IOException {
        return listFiles(path.path());
    }

}
