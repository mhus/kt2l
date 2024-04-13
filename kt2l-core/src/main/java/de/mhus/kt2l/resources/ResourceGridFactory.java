package de.mhus.kt2l.resources;

public interface ResourceGridFactory {

    int DEFAULT_PRIORITY = 100;

    boolean canHandleResourceType(String resourceType);

    ResourcesGrid create(String resourcesType);

    default int getPriority(String resourcesType) {
        return DEFAULT_PRIORITY;
    }
}
