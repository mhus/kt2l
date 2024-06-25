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

package de.mhus.kt2l.resources.deployment;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.commons.tools.MObject;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.ui.UiUtil;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGridWithNamespace;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeploymentGrid extends AbstractGridWithNamespace<DeploymentGrid.Resource, Component, V1Deployment, V1DeploymentList> {

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return DeploymentWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return DeploymentGrid.Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(DeploymentGrid.Resource::getStatus).setHeader("Status").setSortProperty("status");
        resourcesGrid.addColumn(DeploymentGrid.Resource::getReplicas).setHeader("Replicas").setSortProperty("replicas");
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        return switch(sorted) {
            case ("status") -> switch(direction) {
                case ASCENDING -> MObject.compareTo(a.getStatus(), b.getStatus());
                case DESCENDING -> MObject.compareTo(b.getStatus(), a.getStatus());
            };
            case ("replicas") -> switch(direction) {
                case ASCENDING -> MObject.compareTo(a.getReplicas(), b.getReplicas());
                case DESCENDING -> MObject.compareTo(b.getReplicas(), a.getReplicas());
            };
            default -> 0;
        };
    }

    @Override
    protected Resource createResourceItem() {
        return new Resource();
    }

    @Override
    public K8s getManagedType() {
        return K8s.DEPLOYMENT;
    }

    @Getter
    public static class Resource extends ResourceItem<V1Deployment> {
        String status;
        String replicas;

        @Override
        public void updateResource() {
            super.updateResource();
            int ready = resource.getStatus().getReadyReplicas() == null ? 0 : resource.getStatus().getReadyReplicas();
            int replicas = resource.getStatus().getReplicas() == null ? 0 : resource.getStatus().getReplicas();
            this.replicas = ready + "/" + replicas;
            if (replicas == 0)
                status = "Empty";
            else
            if (ready == replicas)
                status = "Ready";
            else
                status = "Not Ready";
            if (ready != replicas)
                setColor(UiUtil.COLOR.RED);
            else if (replicas == 0)
                setColor(UiUtil.COLOR.GREY);
            else
                setColor(null);
        }
    }
}
