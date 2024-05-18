package de.mhus.kt2l.fs;

public interface FileSystemDriver {
    boolean supports(FsConfiguration.Context fsContext);

    FileSystem createFilesystem(FsConfiguration.Context fsContext, String userName);
}
