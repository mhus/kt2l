/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.mhus.kt2l.resources.storageclass;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.commons.tools.MObject;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGridWithoutNamespace;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import io.kubernetes.client.openapi.models.V1PersistentVolumeList;
import io.kubernetes.client.openapi.models.V1StorageClass;
import io.kubernetes.client.openapi.models.V1StorageClassList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class StorageClassGrid extends AbstractGridWithoutNamespace<StorageClassGrid.Resource, Component, V1StorageClass, V1StorageClassList> {

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return StorageClassWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(Resource::getProvisioner).setHeader("Provisioner").setSortProperty("provisioner");
        resourcesGrid.addColumn(Resource::getReclaimPolicy).setHeader("Reclaim Policy").setSortProperty("reclaimPolicy");
        resourcesGrid.addColumn(Resource::getVolumeBindingMode).setHeader("Volume Binding Mode").setSortProperty("volumeBindingMode");
        resourcesGrid.addColumn(Resource::isAllowVolumeExpansion).setHeader("Allow Volume Expansion").setSortProperty("allowVolumeExpansion");
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        if ("status".equals(sorted)) {
            if ("provisioner".equals(sorted)) {
                switch (direction) {
                    case ASCENDING:
                        return MObject.compareTo(a.getProvisioner(), b.getProvisioner());
                    case DESCENDING:
                        return MObject.compareTo(b.getProvisioner(), a.getProvisioner());
                }
            }
            if ("reclaimPolicy".equals(sorted)) {
                switch (direction) {
                    case ASCENDING:
                        return a.getReclaimPolicy().compareTo(b.getReclaimPolicy());
                    case DESCENDING:
                        return b.getReclaimPolicy().compareTo(a.getReclaimPolicy());
                }
            }
            if ("volumeBindingMode".equals(sorted)) {
                switch (direction) {
                    case ASCENDING:
                        return a.getVolumeBindingMode().compareTo(b.getVolumeBindingMode());
                    case DESCENDING:
                        return b.getVolumeBindingMode().compareTo(a.getVolumeBindingMode());
                }
            }
            if ("allowVolumeExpansion".equals(sorted)) {
                switch (direction) {
                    case ASCENDING:
                        return Boolean.compare(a.isAllowVolumeExpansion(), b.isAllowVolumeExpansion());
                    case DESCENDING:
                        return Boolean.compare(b.isAllowVolumeExpansion(), a.isAllowVolumeExpansion());
                }
            }
        }
        return 0;
    }

    @Override
    protected Resource createResourceItem() {
        return new Resource();
    }

    @Override
    public V1APIResource getManagedType() {
        return K8s.STORAGE_CLASS;
    }

    @Getter
    public static class Resource extends ResourceItem<V1StorageClass> {
        String provisioner;
        String reclaimPolicy;
        String volumeBindingMode;
        boolean allowVolumeExpansion;

        @Override
        public void updateResource() {
            super.updateResource();
            this.reclaimPolicy = resource.getReclaimPolicy();
            this.volumeBindingMode = resource.getVolumeBindingMode();
            this.allowVolumeExpansion = resource.getAllowVolumeExpansion();
            this.provisioner = resource.getProvisioner();
        }
    }
}
