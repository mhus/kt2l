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
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.UiUtil;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.node.NodeWatch;
import de.mhus.kt2l.resources.util.AbstractGridWithNamespace;
import de.mhus.kt2l.resources.util.AbstractGridWithoutNamespace;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import static de.mhus.commons.tools.MLang.tryThis;

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
        resourcesGrid.addColumn(DeploymentGrid.Resource::getStatus).setHeader("Status").setSortProperty("status").setSortable(true);
        resourcesGrid.addColumn(DeploymentGrid.Resource::getReplicas).setHeader("Replicas").setSortable(false);
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        if ("status".equals(sorted)) {
            switch (direction) {
                case ASCENDING: return a.getStatus().compareTo(b.getStatus());
                case DESCENDING: return b.getStatus().compareTo(a.getStatus());
            }
        }
        return 0;
    }

    @Override
    protected Resource createResourceItem() {
        return new Resource();
    }

    @Override
    public K8s getManagedResourceType() {
        return K8s.DEPLOYMENT;
    }

    @Getter
    public static class Resource extends ResourceItem<V1Deployment> {
        String status;
        String replicas;

        @Override
        public void updateResource() {
            this.replicas = resource.getStatus().getReadyReplicas() + "/" + resource.getStatus().getReplicas();
            if (resource.getStatus().getReplicas() == 0)
                status = "Empty";
            else
            if (resource.getStatus().getReadyReplicas() == resource.getStatus().getReplicas())
                status = "Ready";
            else
                status = "Not Ready";
            if (resource.getStatus().getReadyReplicas() != resource.getStatus().getReplicas())
                setColor(UiUtil.COLOR.RED);
            else
                setColor(null);
        }
    }
}
