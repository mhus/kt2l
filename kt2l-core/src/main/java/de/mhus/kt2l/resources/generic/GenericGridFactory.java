package de.mhus.kt2l.resources.generic;

import de.mhus.kt2l.resources.ResourceGridFactory;
import de.mhus.kt2l.resources.ResourcesGrid;

public class GenericGridFactory implements ResourceGridFactory {
    @Override
    public boolean canHandleResourceType(String resourceType) {
        return true;
    }

    @Override
    public ResourcesGrid create(String resourcesType) {
        var grid = new GenericGrid();
        grid.setResourceType(resourcesType);
        return grid;
    }

    public int getPriority(String resourcesType) {
        return Integer.MAX_VALUE;
    }

}
