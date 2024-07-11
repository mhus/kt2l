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
import de.mhus.commons.lang.IRegistration;
import de.mhus.commons.lang.OptionalBoolean;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.pod.PodWatch;
import de.mhus.kt2l.resources.util.AbstractGridWithoutNamespace;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Watch;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class NodeGrid extends AbstractGridWithoutNamespace<NodeGrid.Resource, Component, V1Node, V1NodeList> {

    private HandlerK8s podResourceHandler;
    private List<V1Pod> podList;
    private IRegistration podEventRegistration;

    @Override
    protected void init() {
        super.init();
        this.podEventRegistration = panel.getCore().backgroundJobInstance(
                panel.getCluster(),
                PodWatch.class
        ).getEventHandler().registerWeak(this::changePodEvent);
        podList = Collections.synchronizedList(new LinkedList<V1Pod>());
        this.podResourceHandler = k8sService.getTypeHandler(K8s.POD);
        try {
            var pl = podResourceHandler.createResourceListWithoutNamespace(panel.getCluster().getApiProvider());
            pl.getItems().forEach(pod -> podList.add((V1Pod)pod));
        } catch (Exception e) {
            LOGGER.error("init", e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (podEventRegistration != null) podEventRegistration.unregister();
        podEventRegistration = null;
    }

    private void changePodEvent(Watch.Response<V1Pod> event) {
        if (event.object == null) return;
        switch (event.type) {
            case "ADDED":
                podList.add(event.object);
                break;
            case "DELETED":
                for (int i = 0; i < podList.size(); i++) {
                    if (podList.get(i).getMetadata().getUid().equals(event.object.getMetadata().getUid())) {
                        podList.remove(i);
                        break;
                    }
                }
                break;
            default:
                return;
        }
        var nodeName = event.object.getSpec().getNodeName();
        var node = resourcesList.stream().filter(r -> r.getName().equals(nodeName)).findFirst();
        if (node.isPresent()) {
            node.get().updateResource();
            panel.getCore().ui().access(() -> {
                resourcesGrid.getDataProvider().refreshItem(node.get());
            });
        }
    }

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
        resourcesGrid.addColumn(NodeGrid.Resource::getStatus).setHeader("Status").setSortProperty("status");
        resourcesGrid.addColumn(NodeGrid.Resource::getTaintCnt).setHeader("Taints").setSortProperty("taints");
        resourcesGrid.addColumn(NodeGrid.Resource::getPods).setHeader("Pods").setSortProperty("pods");
        resourcesGrid.addColumn(NodeGrid.Resource::getIp).setHeader("IP").setSortProperty("ip");
        resourcesGrid.addColumn(NodeGrid.Resource::getVersion).setHeader("Version").setSortProperty("version");
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
        if ("pods".equals(sorted)) {
            switch (direction) {
                case ASCENDING:
                    return Long.compare(a.getPods(), b.getPods());
                case DESCENDING:
                    return Long.compare(b.getPods(), a.getPods());
            }
        }
        return 0;
    }

    @Override
    protected Resource createResourceItem() {
        return new Resource();
    }

    @Override
    public V1APIResource getManagedType() {
        return K8s.NODE;
    }

    @Getter
    public static class Resource extends AbstractGridWithoutNamespace.ResourceItem<V1Node> {
        String status;
        private int taintCnt;
        private String ip;
        private String version;
        private long pods;

        @Override
        public void updateResource() {
            super.updateResource();
            StringBuilder s = new StringBuilder();
            if (resource.getSpec().getTaints() != null) {
                for (var taint : resource.getSpec().getTaints()) {
                    if (s.length() > 0) s.append(",");
                    s.append(taint.getEffect()).append(":").append(taint.getKey());
                }
            } else {
                s.append("Ready");
            }
            this.status = s.toString();
            this.pods = ((NodeGrid)getGrid()).podList.stream().filter(p -> p.getSpec().getNodeName().equals(resource.getMetadata().getName())).count();
            this.taintCnt = tryThis(() -> resource.getSpec().getTaints().size()).orElse(0);
            this.ip = resource.getStatus().getAddresses().stream()
                    .filter(a -> "InternalIP".equals(a.getType()))
                    .findFirst()
                    .map(a -> a.getAddress())
                    .orElse(null);
            this.version = tryThis(() -> resource.getStatus().getNodeInfo().getKubeletVersion()).orElse("");
        }
    }
}
