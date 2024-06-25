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

package de.mhus.kt2l.resources.persistentvolume;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGridWithoutNamespace;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import io.kubernetes.client.openapi.models.V1PersistentVolumeList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class PersistentVolumeGrid extends AbstractGridWithoutNamespace<PersistentVolumeGrid.Resource, Component, V1PersistentVolume, V1PersistentVolumeList> {

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return PersistentVolumeWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(Resource::getCapacity).setHeader("Capacity").setSortProperty("capacity");
        resourcesGrid.addColumn(Resource::getAccessModes).setHeader("Access Modes").setSortProperty("accessModes");
        resourcesGrid.addColumn(Resource::getReclaimPolicy).setHeader("Reclaim Policy").setSortProperty("reclaimPolicy");
        resourcesGrid.addColumn(Resource::getStatus).setHeader("Status").setSortProperty("status");
        resourcesGrid.addColumn(Resource::getClaim).setHeader("Claim").setSortProperty("claim");
        resourcesGrid.addColumn(Resource::getStorageClass).setHeader("Storage Class").setSortProperty("storageClass");
        resourcesGrid.addColumn(Resource::getReason).setHeader("Reason").setSortProperty("reason");
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        if ("status".equals(sorted)) {
            switch (direction) {
                case ASCENDING: return a.getStatus().compareTo(b.getStatus());
                case DESCENDING: return b.getStatus().compareTo(a.getStatus());
            }
        }
        if ("capacity".equals(sorted)) {
            switch (direction) {
                case ASCENDING: return a.getCapacity().compareTo(b.getCapacity());
                case DESCENDING: return b.getCapacity().compareTo(a.getCapacity());
            }
        }
        if ("accessModes".equals(sorted)) {
            switch (direction) {
                case ASCENDING: return a.getAccessModes().compareTo(b.getAccessModes());
                case DESCENDING: return b.getAccessModes().compareTo(a.getAccessModes());
            }
        }
        if ("reclaimPolicy".equals(sorted)) {
            switch (direction) {
                case ASCENDING: return a.getReclaimPolicy().compareTo(b.getReclaimPolicy());
                case DESCENDING: return b.getReclaimPolicy().compareTo(a.getReclaimPolicy());
            }
        }
        if ("claim".equals(sorted)) {
            switch (direction) {
                case ASCENDING: return a.getClaim().compareTo(b.getClaim());
                case DESCENDING: return b.getClaim().compareTo(a.getClaim());
            }
        }
        if ("storageClass".equals(sorted)) {
            switch (direction) {
                case ASCENDING: return a.getStorageClass().compareTo(b.getStorageClass());
                case DESCENDING: return b.getStorageClass().compareTo(a.getStorageClass());
            }
        }
        if ("reason".equals(sorted)) {
            switch (direction) {
                case ASCENDING: return a.getReason().compareTo(b.getReason());
                case DESCENDING: return b.getReason().compareTo(a.getReason());
            }
        }
        return 0;
    }

    @Override
    protected Resource createResourceItem() {
        return new Resource();
    }

    @Override
    public K8s getManagedType() {
        return K8s.PERSISTENT_VOLUME;
    }

    @Getter
    public static class Resource extends ResourceItem<V1PersistentVolume> {
        String capacity;
        String accessModes;
        String storageClass;
        String reclaimPolicy;
        String status;
        String claim;
        String reason;

        @Override
        public void updateResource() {
            super.updateResource();
            this.status = resource.getStatus().getPhase();
            this.capacity = tryThis(() -> MString.toByteDisplayString(resource.getSpec().getCapacity().get("storage").getNumber().longValue())).or("");
            this.accessModes = String.join(", ", resource.getSpec().getAccessModes());
            this.storageClass = resource.getSpec().getStorageClassName();
            this.reclaimPolicy = resource.getSpec().getPersistentVolumeReclaimPolicy();
            this.claim = resource.getSpec().getClaimRef() == null ? "" : resource.getSpec().getClaimRef().getName();
            this.reason = resource.getStatus().getReason();
        }
    }
}
