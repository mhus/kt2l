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

package de.mhus.kt2l.resources.node;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGridWithoutNamespace;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class NodeGrid extends AbstractGridWithoutNamespace<NodeGrid.Resource, Component, V1Node, V1NodeList> {

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return NodeWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return NodeGrid.Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(NodeGrid.Resource::getStatus).setHeader("Status").setSortable(true);
        resourcesGrid.addColumn(NodeGrid.Resource::getTaintCnt).setHeader("Taints").setSortable(true);
        resourcesGrid.addColumn(NodeGrid.Resource::getIp).setHeader("IP").setSortable(true);
        resourcesGrid.addColumn(NodeGrid.Resource::getVersion).setHeader("Version").setSortable(true);
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        if ("status".equals(sorted)) {
            switch (direction) {
                case ASCENDING: return a.getStatus().compareTo(b.getStatus());
                case DESCENDING: return b.getStatus().compareTo(a.getStatus());
            }
        }
        if ("taints".equals(sorted)) {
            switch (direction) {
                case ASCENDING: return Integer.compare(a.getTaintCnt(), b.getTaintCnt());
                case DESCENDING: return Integer.compare(b.getTaintCnt(), a.getTaintCnt());
            }
        }
        if ("ip".equals(sorted)) {
            switch (direction) {
                case ASCENDING:
                    return Objects.compare(a.getIp(), b.getIp(), String::compareTo);
                case DESCENDING:
                    return Objects.compare(b.getIp(), a.getIp(), String::compareTo);
            }
        }
        if ("version".equals(sorted)) {
            switch (direction) {
                case ASCENDING:
                    return Objects.compare(a.getVersion(), b.getVersion(), String::compareTo);
                case DESCENDING:
                    return Objects.compare(b.getVersion(), a.getVersion(), String::compareTo);
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
        return K8s.NODE;
    }

    @Getter
    public static class Resource extends AbstractGridWithoutNamespace.ResourceItem<V1Node> {
        String status;
        private int taintCnt;
        private String ip;
        private String version;

        @Override
        public void updateResource() {
            this.status = resource.getStatus().getPhase();
            this.taintCnt = tryThis(() -> resource.getSpec().getTaints().size()).or(0);
            this.ip = resource.getStatus().getAddresses().stream()
                    .filter(a -> "InternalIP".equals(a.getType()))
                    .findFirst()
                    .map(a -> a.getAddress())
                    .orElse(null);
            this.version = tryThis(() -> resource.getStatus().getNodeInfo().getKubeletVersion()).or("");
        }
    }
}
