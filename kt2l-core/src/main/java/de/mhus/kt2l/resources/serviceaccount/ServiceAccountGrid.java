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

package de.mhus.kt2l.resources.serviceaccount;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.commons.tools.MObject;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGridWithNamespace;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1ServiceAccount;
import io.kubernetes.client.openapi.models.V1ServiceAccountList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceAccountGrid extends AbstractGridWithNamespace<ServiceAccountGrid.Resource, Component, V1ServiceAccount, V1ServiceAccountList> {

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return ServiceAccountWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(Resource::isAutoMount).setHeader("Auto Mount").setSortProperty("autoMount");
        resourcesGrid.addColumn(Resource::getSecrets).setHeader("Secrets").setSortProperty("secrets");
        resourcesGrid.addColumn(Resource::getImagePullSecrets).setHeader("Image Pull Secrets").setSortProperty("imagePullSecrets");
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        return switch(sorted) {
            case ("autoMount") ->
                switch (direction) {
                    case ASCENDING -> MObject.compareTo(a.isAutoMount(), b.isAutoMount());
                    case DESCENDING -> MObject.compareTo(b.isAutoMount(), a.isAutoMount());
                };
            case ("secrets") ->
                switch (direction) {
                    case ASCENDING -> MObject.compareTo(a.getSecrets(), b.getSecrets());
                    case DESCENDING -> MObject.compareTo(b.getSecrets(), a.getSecrets());
                };
            case ("imagePullSecrets") ->
                switch (direction) {
                    case ASCENDING -> MObject.compareTo(a.getImagePullSecrets(), b.getImagePullSecrets());
                    case DESCENDING -> MObject.compareTo(b.getImagePullSecrets(), a.getImagePullSecrets());
                };
            default -> 0;
        };
    }

    @Override
    protected Resource createResourceItem() {
        return new Resource();
    }

    @Override
    public V1APIResource getManagedType() {
        return K8s.SERVICE_ACCOUNT;
    }

    @Getter
    public static class Resource extends ResourceItem<V1ServiceAccount> {
        boolean autoMount;
        int secrets;
        int imagePullSecrets;

        @Override
        public void updateResource() {
            super.updateResource();
            autoMount = resource.getAutomountServiceAccountToken() != null && resource.getAutomountServiceAccountToken();
            secrets = resource.getSecrets() == null ? 0 : resource.getSecrets().size();
            imagePullSecrets = resource.getImagePullSecrets() == null ? 0 : resource.getImagePullSecrets().size();
        }

    }
}
