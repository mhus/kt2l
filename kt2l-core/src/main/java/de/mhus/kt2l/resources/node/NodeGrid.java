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
import de.mhus.commons.tools.MCast;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.pod.PodWatch;
import de.mhus.kt2l.resources.util.AbstractGridWithoutNamespace;
import io.kubernetes.client.Metrics;
import io.kubernetes.client.custom.NodeMetrics;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Watch;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class NodeGrid extends AbstractGridWithoutNamespace<NodeGrid.Resource, Component, V1Node, V1NodeList> {

    private HandlerK8s podResourceHandler;
    private Map<String, V1Pod> podMap;
    private IRegistration podEventRegistration;
    private long disableMetricsUntil;

    @Override
    protected void init() {
        super.init();
        this.podEventRegistration = panel.getCore().backgroundJobInstance(
                panel.getCluster(),
                PodWatch.class
        ).getEventHandler().registerWeak(this::changePodEvent);
        podMap = Collections.synchronizedMap(new HashMap<>());
        this.podResourceHandler = k8sService.getTypeHandler(K8s.POD);
        try {
            var pl = podResourceHandler.createResourceListWithoutNamespace(panel.getCluster().getApiProvider());
            pl.getItems().forEach(pod -> podMap.put(pod.getMetadata().getUid(), (V1Pod)pod));
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
                try {
                    podMap.put(event.object.getMetadata().getUid(), event.object);
                    var nodeName = event.object.getSpec().getNodeName();
                    updateNodePods(nodeName);
                } catch (Exception e) {
                    LOGGER.error("pod put", e);
                }
                break;
            case "DELETED":
                try {
                    podMap.remove(event.object.getMetadata().getUid());
                    var nodeName = event.object.getSpec().getNodeName();
                    updateNodePods(nodeName);
                } catch (Exception e) {
                    LOGGER.error("pod delete", e);
                }
                break;
            case "MODIFIED":
                try {
                    var old = podMap.put(event.object.getMetadata().getUid(), event.object);
                    var oldNodeName = tryThis(() -> old.getSpec().getNodeName()).orElse("");
                    var newNodeName = event.object.getSpec().getNodeName();
                    if (!Objects.equals(oldNodeName, newNodeName)) {
                        updateNodePods(oldNodeName);
                        updateNodePods(newNodeName);
                    }
                } catch (Exception e) {
                    LOGGER.error("pod modified", e);
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

    private void updateNodePods(String nodeName) {
        resourcesList.stream().filter(r -> r.getName().equals(nodeName)).findFirst().ifPresent(r -> {
            r.updatePods();
            panel.getCore().ui().access(() -> resourcesGrid.getDataProvider().refreshItem(r));
        });
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
        resourcesGrid.addColumn(NodeGrid.Resource::getPods).setHeader("Pods").setSortProperty("pods");
        resourcesGrid.addColumn(NodeGrid.Resource::getDaemonPods).setHeader("D").setSortProperty("daemonPods").setTooltipGenerator(r -> "Daemon Pods");
        resourcesGrid.addColumn(NodeGrid.Resource::getPendingPods).setHeader("P").setSortProperty("pendingPods").setTooltipGenerator(r -> "Pending Pods");
        resourcesGrid.addColumn(NodeGrid.Resource::getRunningPods).setHeader("R").setSortProperty("runningPods").setTooltipGenerator(r -> "Running Pods");
        resourcesGrid.addColumn(NodeGrid.Resource::getTerminatingPods).setHeader("T").setSortProperty("terminatingPods").setTooltipGenerator(r -> "Terminating Pods");
        if (cluster.isMetricsEnabled()) {
            resourcesGrid.addColumn(Resource::getMetricCpuString).setHeader("CPU").setSortProperty("cpu");
            resourcesGrid.addColumn(res -> res.getMetricCpuPercentage() < 0 ? "" : res.getMetricCpuPercentage()).setHeader("CPU%").setSortProperty("cpu%");
            resourcesGrid.addColumn(Resource::getMetricMemoryString).setHeader("Mem").setSortProperty("memory");
            resourcesGrid.addColumn(res -> res.getMetricMemoryPercentage() < 0 ? "" : res.getMetricMemoryPercentage()).setHeader("Mem%").setSortProperty("memory%");
        }
        resourcesGrid.addColumn(NodeGrid.Resource::getIp).setHeader("IP").setSortProperty("ip");
        resourcesGrid.addColumn(NodeGrid.Resource::getTaintCnt).setHeader("Taints").setSortProperty("taints");
        resourcesGrid.addColumn(NodeGrid.Resource::getStatus).setHeader("Status").setSortProperty("status");
        resourcesGrid.addColumn(NodeGrid.Resource::getVersion).setHeader("Version").setSortProperty("version");
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        if ("status".equals(sorted)) {
            return switch (direction) {
                case ASCENDING -> a.getStatus().compareTo(b.getStatus());
                case DESCENDING -> b.getStatus().compareTo(a.getStatus());
            };
        }
        if ("taints".equals(sorted)) {
            return switch (direction) {
                case ASCENDING -> Integer.compare(a.getTaintCnt(), b.getTaintCnt());
                case DESCENDING -> Integer.compare(b.getTaintCnt(), a.getTaintCnt());
            };
        }
        if ("ip".equals(sorted)) {
            return switch (direction) {
                case ASCENDING -> Objects.compare(a.getIp(), b.getIp(), String::compareTo);
                case DESCENDING -> Objects.compare(b.getIp(), a.getIp(), String::compareTo);
            };
        }
        if ("version".equals(sorted)) {
            return switch (direction) {
                case ASCENDING -> Objects.compare(a.getVersion(), b.getVersion(), String::compareTo);
                case DESCENDING -> Objects.compare(b.getVersion(), a.getVersion(), String::compareTo);
            };
        }
        if ("pods".equals(sorted)) {
            return switch (direction) {
                case ASCENDING -> Long.compare(a.getPods(), b.getPods());
                case DESCENDING -> Long.compare(b.getPods(), a.getPods());
            };
        }
        if ("daemonPods".equals(sorted)) {
            return switch (direction) {
                case ASCENDING -> Long.compare(a.getDaemonPods(), b.getDaemonPods());
                case DESCENDING -> Long.compare(b.getDaemonPods(), a.getDaemonPods());
            };
        }
        if ("pendingPods".equals(sorted)) {
            return switch (direction) {
                case ASCENDING -> Long.compare(a.getPendingPods(), b.getPendingPods());
                case DESCENDING -> Long.compare(b.getPendingPods(), a.getPendingPods());
            };
        }
        if ("runningPods".equals(sorted)) {
            return switch (direction) {
                case ASCENDING -> Long.compare(a.getRunningPods(), b.getRunningPods());
                case DESCENDING -> Long.compare(b.getRunningPods(), a.getRunningPods());
            };
        }
        if ("terminatingPods".equals(sorted)) {
            return switch (direction) {
                case ASCENDING -> Long.compare(a.getTerminatingPods(), b.getTerminatingPods());
                case DESCENDING -> Long.compare(b.getTerminatingPods(), a.getTerminatingPods());
            };
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

    @Override
    public void refresh(long counter) {
        super.refresh(counter);
        if (counter % 10 == 1)
            updateMetrics();
    }

    protected synchronized void updateMetrics() {
        if (filteredList == null) return;
        if (!cluster.isMetricsEnabled()) return;

        var metrics = getNodeMetrics();
        var map = metrics.stream().collect(Collectors.toMap(m -> m.getMetadata().getName(), m -> m));

        final AtomicBoolean changed = new AtomicBoolean(false);
        resourcesList.stream().forEach(r -> {
            var m = map.get(r.getName());
            if (r.setMetrics(m)) {
                resourcesGrid.getDataProvider().refreshItem(r);
                changed.set(true);
            }
        });

        if (changed.get()) {
            getPanel().getCore().ui().push();
        }
    }

    @SuppressWarnings("unchecked")
    private List<NodeMetrics> getNodeMetrics() {
        if (disableMetricsUntil != 0 && disableMetricsUntil > System.currentTimeMillis())
            return Collections.EMPTY_LIST;
        Metrics metrics = new Metrics(panel.getCluster().getApiProvider().getClient());
        try {
            var list = metrics.getNodeMetrics();
            return list.getItems();
        } catch (ApiException e) {
            LOGGER.error("Can't get metrics for nodes with RC {}", e.getCode(), e);
            panel.getCluster().getApiProvider().invalidate();
            disableMetricsUntil = System.currentTimeMillis() + 60000;
        } catch (Exception e) {
            LOGGER.error("Can't get metrics for nodes",e);
        }
        return Collections.EMPTY_LIST;
    }

    @Getter
    public static class Resource extends AbstractGridWithoutNamespace.ResourceItem<V1Node> {
        String status;
        private int taintCnt;
        private String ip;
        private String version;
        private long pods;
        private long daemonPods;
        private long pendingPods;
        private long runningPods;
        private long terminatingPods;
        private double metricCpu = Double.MAX_VALUE;
        private long metricMemory = Long.MAX_VALUE;
        private String metricCpuString = "-";
        private String metricMemoryString = "-";
        private int metricCpuPercentage = -1;
        private int metricMemoryPercentage = -1;
        private NodeMetrics metric;

        @Override
        public void updateResource() {
            super.updateResource();
            StringBuilder s = new StringBuilder();
            if (resource.getSpec().getTaints() != null) {
                for (var taint : resource.getSpec().getTaints()) {
                    if (!s.isEmpty()) s.append(",");
                    s.append(taint.getEffect()).append(":").append(taint.getKey());
                }
            } else {
                s.append("Ready");
            }
            this.status = s.toString();
            updatePods();
            this.taintCnt = tryThis(() -> resource.getSpec().getTaints().size()).orElse(0);
            this.ip = resource.getStatus().getAddresses().stream()
                    .filter(a -> "InternalIP".equals(a.getType()))
                    .findFirst()
                    .map(a -> a.getAddress())
                    .orElse(null);
            this.version = tryThis(() -> resource.getStatus().getNodeInfo().getKubeletVersion()).orElse("");
        }

        public void updatePods() {
            this.pods = tryThis(() -> ((NodeGrid) getGrid()).podMap.values().stream().filter(p ->
                    Objects.equals(p.getSpec().getNodeName(), resource.getMetadata().getName())
            ).count()).orElse(-1L);
            this.daemonPods = tryThis(() -> ((NodeGrid) getGrid()).podMap.values().stream().filter(p ->
                    Objects.equals(p.getSpec().getNodeName(), resource.getMetadata().getName())
                            &&
                            p.getMetadata().getOwnerReferences().stream().filter(r -> r.getKind().equals("DaemonSet")).findFirst().isPresent()
            ).count()).orElse(-1L);
            this.pendingPods = tryThis(() -> ((NodeGrid) getGrid()).podMap.values().stream().filter(p ->
                    Objects.equals(p.getSpec().getNodeName(), resource.getMetadata().getName())
                            &&
                            "xxx".equals(p.getStatus().getPhase()) // XXX
            ).count()).orElse(-1L);
            this.runningPods = tryThis(() -> ((NodeGrid) getGrid()).podMap.values().stream().filter(p ->
                    Objects.equals(p.getSpec().getNodeName(), resource.getMetadata().getName())
                            &&
                            "Running".equals(p.getStatus().getPhase())
                            &&
                            p.getMetadata().getDeletionTimestamp() == null
            ).count()).orElse(-1L);
            this.terminatingPods = tryThis(() -> ((NodeGrid) getGrid()).podMap.values().stream().filter(p ->
                    Objects.equals(p.getSpec().getNodeName(), resource.getMetadata().getName())
                            &&
                            p.getMetadata().getDeletionTimestamp() != null
            ).count()).orElse(-1L);
        }

        public synchronized boolean setMetrics(NodeMetrics metric) {
            if (metric == null) return false;
            this.metric = metric;

            double cpu = metric.getUsage().get("cpu").getNumber().doubleValue();
            long mem = metric.getUsage().get("memory").getNumber().longValue();
            var cpuString = MString.truncate(String.valueOf(cpu), 5);
            var memoryString = MCast.toByteUnit(mem);
            if (cpuString.equals(metricCpuString) && memoryString.equals(metricMemoryString)) return false;
            metricCpuString = cpuString;
            metricMemoryString = memoryString;
            metricCpu = cpu;
            metricMemory = mem;

            metricCpuPercentage = -1;
            metricMemoryPercentage = -1;
            double cpuLimit = resource.getStatus().getCapacity().get("cpu").getNumber().doubleValue();
            long memLimit = resource.getStatus().getCapacity().get("memory").getNumber().longValue();
            if (cpuLimit > 0)
                metricCpuPercentage = (int) (cpu * 100 / cpuLimit);
            if (memLimit > 0)
                metricMemoryPercentage = (int) (mem * 100 / memLimit);

            return true;
        }
    }
}
