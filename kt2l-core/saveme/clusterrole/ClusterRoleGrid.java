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
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.commons.lang.IRegistration;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGrid;
import de.mhus.kt2l.resources.util.AbstractGridWithoutNamespace;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.RbacAuthorizationV1Api;
import io.kubernetes.client.openapi.models.V1ClusterRole;
import io.kubernetes.client.openapi.models.V1ClusterRoleList;
import io.kubernetes.client.util.Watch;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class ClusterRoleGrid extends AbstractGridWithoutNamespace<ClusterRoleGrid.Resource, Component, V1ClusterRole, V1ClusterRoleList> {

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return ClusterRoleWatch.class;
    }

    protected void createGridColumnsAfterName(Grid<ClusterRoleGrid.Resource> resourcesGrid) {
        resourcesGrid.addColumn(Resource::getStatus).setHeader("Status").setSortable(true);
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        if ("status".equals(sorted)) {
            switch (direction) {
                case SortDirection.ASCENDING: return a.getStatus().compareTo(b.getStatus());
                case SortDirection.DESCENDING: return b.getStatus().compareTo(a.getStatus());
            }
        }
        return 0;
    }

    @Override
    protected Resource createResourceItem(V1ClusterRole object) {
        return new Resource(object);
    }

    @Override
    public K8s.RESOURCE getManagedResourceType() {
        return K8s.RESOURCE.CLUSTER_ROLE;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected V1ClusterRoleList createRawResourceList() throws ApiException {
        RbacAuthorizationV1Api authenticationV1Api = new RbacAuthorizationV1Api(cluster.getApiProvider().getClient());
        return authenticationV1Api.listClusterRole().execute();
    }

    @Getter
    public static class Resource extends AbstractGridWithoutNamespace.ResourceItem<V1ClusterRole> {
        String status;

        public Resource(V1ClusterRole resource) {
            super(resource);
            this.status = resource.getMetadata().getDeletionTimestamp() == null ? "Active" : "Deleted";
        }
    }
}
