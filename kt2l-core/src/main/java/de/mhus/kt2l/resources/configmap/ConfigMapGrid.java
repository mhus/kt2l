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

package de.mhus.kt2l.resources.configmap;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.commons.lang.IRegistration;
import de.mhus.commons.tools.MLang;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGrid;
import de.mhus.kt2l.resources.util.AbstractGridWithNamespace;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.kubernetes.client.util.Watch;
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
public class ConfigMapGrid extends AbstractGridWithNamespace<ConfigMapGrid.Resource, Component, V1ConfigMap, V1ConfigMapList> {
    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return ConfigMapWatch.class;
    }

    @Override
    protected Resource createResourceItem(V1ConfigMap object) {
        return new Resource(object);
    }

    @Override
    public K8s.RESOURCE getManagedResourceType() {
        return K8s.RESOURCE.CONFIG_MAP;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {

    }

    @Override
    protected V1ConfigMapList createRawResourceListForNamespace(String namespace) throws ApiException {
        return cluster.getApiProvider().getCoreV1Api().listNamespacedConfigMap(namespace).execute();
    }

    @Override
    protected V1ConfigMapList createRawResourceListForAllNamespaces() throws ApiException {
        return cluster.getApiProvider().getCoreV1Api().listConfigMapForAllNamespaces().execute();
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        return 0;
    }

    @Getter
    public static class Resource extends AbstractGridWithNamespace.ResourceItem<V1ConfigMap> {

        public Resource(V1ConfigMap resource) {
            super(resource);
        }
    }
}
