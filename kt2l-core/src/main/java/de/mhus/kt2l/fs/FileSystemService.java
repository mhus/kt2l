package de.mhus.kt2l.fs;

import de.mhus.commons.errors.NotFoundRuntimeException;
import de.mhus.commons.util.SoftHashMap;
import de.mhus.kt2l.core.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FileSystemService {

    @Autowired
    private FsConfiguration fsConfiguration;

    @Autowired
    private List<FileSystemDriver> drivers;

    private final SoftHashMap<String, FileSystem> cache = new SoftHashMap<>();

    public FileSystem getFileSystem() {
        final var userName = SecurityContext.lookupUserName();
        final var fsContext = fsConfiguration.getFsContextForUser(userName);
        synchronized (cache) {
            return cache.computeIfAbsent(userName, k -> createFileSystem(fsContext, userName));
        }
    }

    private FileSystem createFileSystem(FsConfiguration.Context fsContext, String userName) {
        for (var driver : drivers) {
            if (driver.supports(fsContext)) {
                return driver.createFilesystem(fsContext, userName);
            }
        }
        throw new NotFoundRuntimeException("No driver found for " + fsContext);
    }

}
