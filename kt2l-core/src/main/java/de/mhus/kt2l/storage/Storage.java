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
        return new OutputFile(this, path.getPath() + "/" + name, name, false, 0, System.currentTimeMillis(), createFileStream(path.getPath() + "/" + name));
    }

    public OutputFile createFileStream(String context, String name) throws IOException {
        final var path = createFilePath(context);
        name = MFile.normalize(name);
        return new OutputFile(this, path + "/" + name, name, false, 0, System.currentTimeMillis(), createFileStream(path));
    }

    public InputFile openFile(String path) throws IOException {
        return new InputFile(this, path, path, false, 0, 0, openFileStream(path));
    }

    /**
     *
     * @param path
     * @return
     * @throws IOException, FileNotFoundException if file was not found.
     */
    protected abstract InputStream openFileStream(String path) throws IOException;

    private String createFilePath(String context) {
        return dateFormat.format(new Date()) + ULID.random() + "_" + MFile.normalize(context);
    }

    protected abstract OutputStream createFileStream(String path) throws IOException;

    protected OutputStream createFileStream(StorageFile path) throws IOException {
        return createFileStream(path.getPath());
    }

    /**
     *
     * @param path
     * @return
     * @throws IOException, FileNotFoundException if directory was not found.
     */
    public abstract List<StorageFile> listFiles(String path) throws IOException;

    public List<StorageFile> listFiles(StorageFile path) throws IOException {
        return listFiles(path.getPath() == null ? "" : path.getPath());
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
        return new StorageFile(this, path, MString.afterLastIndex(path, '/'), true, 0, System.currentTimeMillis());
    }
}
