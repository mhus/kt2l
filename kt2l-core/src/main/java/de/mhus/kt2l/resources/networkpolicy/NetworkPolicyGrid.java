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

package de.mhus.kt2l.resources.networkpolicy;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGridWithNamespace;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1NetworkPolicy;
import io.kubernetes.client.openapi.models.V1NetworkPolicyList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class NetworkPolicyGrid extends AbstractGridWithNamespace<NetworkPolicyGrid.Resource, Component, V1NetworkPolicy, V1NetworkPolicyList> {

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return NetworkPolicyWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(Resource::getIngPorts).setHeader("ING-Ports").setSortable(false);
        resourcesGrid.addColumn(Resource::getIngBlock).setHeader("ING-Block").setSortable(false);
        resourcesGrid.addColumn(Resource::getEgrPorts).setHeader("EGR-Ports").setSortable(false);
        resourcesGrid.addColumn(Resource::getEgrBlock).setHeader("EGR-Block").setSortable(false);
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
        return K8s.NETWORK_POLICY;
    }

    @Getter
    public static class Resource extends ResourceItem<V1NetworkPolicy> {
        String ingPorts;
        String ingBlock;
        String egrPorts;
        String egrBlock;

        @Override
        public void updateResource() {
            super.updateResource();
            ingPorts = tryThis(() -> getResource().getSpec().getIngress().stream().map(i -> i.getPorts().toString()).reduce("", (a, b) -> a + ", " + b)).orElse("");
            ingBlock = tryThis(() -> getResource().getSpec().getIngress().stream().map(i -> i.getFrom().toString()).reduce("", (a, b) -> a + ", " + b)).orElse("");
            egrPorts = tryThis(() -> getResource().getSpec().getEgress().stream().map(i -> i.getPorts().toString()).reduce("", (a, b) -> a + ", " + b)).orElse("");
            egrBlock = tryThis(() -> getResource().getSpec().getEgress().stream().map(i -> i.getTo().toString()).reduce("", (a, b) -> a + ", " + b)).orElse("");
        }

    }
}
