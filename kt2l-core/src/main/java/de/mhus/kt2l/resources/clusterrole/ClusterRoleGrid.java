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

package de.mhus.kt2l.resources.clusterrole;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGridWithoutNamespace;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1ClusterRole;
import io.kubernetes.client.openapi.models.V1ClusterRoleList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class ClusterRoleGrid extends AbstractGridWithoutNamespace<ClusterRoleGrid.Resource, Component, V1ClusterRole, V1ClusterRoleList> {

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return ClusterRoleWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(Resource::getResources).setHeader("Resources").setSortable(false);
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        return 0;
    }

    @Override
    protected Resource createResourceItem() {
        return new Resource();
    }

    @Override
    public V1APIResource getManagedType() {
        return K8s.CLUSTER_ROLE;
    }

    @Getter
    public static class Resource extends ResourceItem<V1ClusterRole> {
        String resources;

        @Override
        public void updateResource() {
            super.updateResource();
            resources = tryThis(()-> resource.getRules().stream().map(rule -> rule.getResources().toString()).reduce("", (a, b) -> a + b)).or("");
        }
    }
}
