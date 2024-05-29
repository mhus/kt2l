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

package de.mhus.kt2l.resources.service;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.commons.tools.MObject;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.UiUtil;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGridWithNamespace;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1DaemonSetList;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceGrid extends AbstractGridWithNamespace<ServiceGrid.Resource, Component, V1Service, V1ServiceList> {

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return ServiceWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(Resource::getType).setHeader("Type").setSortProperty("type").setSortable(true);
        resourcesGrid.addColumn(Resource::getClusterIp).setHeader("ClusterIp").setSortProperty("clusterip").setSortable(true);
        resourcesGrid.addColumn(Resource::getPorts).setHeader("Ports").setSortable(false);
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        if ("type".equals(sorted)) {
            switch (direction) {
                case ASCENDING: return a.getType().compareTo(b.getType());
                case DESCENDING: return b.getType().compareTo(a.getType());
            }
        } else
        if ("clusterip".equals(sorted)) {
            switch (direction) {
                case ASCENDING: return MObject.compareTo(a.getClusterIp(), b.getClusterIp());
                case DESCENDING: return MObject.compareTo(b.getClusterIp(), a.getClusterIp());
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
        return K8s.SERVICE;
    }

    @Getter
    public static class Resource extends ResourceItem<V1Service> {
        String type;
        String clusterIp;
        String ports;

        @Override
        public void updateResource() {
            type = resource.getSpec().getType();
            clusterIp = resource.getSpec().getClusterIP();
            ports = resource.getSpec().getPorts().stream()
                    .map(p -> (p.getName() != null ? p.getName() + ":" : "") + p.getPort() + "/" + p.getTargetPort())
                    .reduce((a, b) -> a + " " + b)
                    .orElse("");
            setColor(null);
        }

    }
}
