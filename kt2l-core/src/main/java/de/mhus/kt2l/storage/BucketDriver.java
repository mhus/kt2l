package de.mhus.kt2l.storage;

public interface BucketDriver {
    boolean supports(StorageConfiguration.Bucket fsContext);

    Storage createStorage(StorageConfiguration.Bucket fsContext, String userName);
}
