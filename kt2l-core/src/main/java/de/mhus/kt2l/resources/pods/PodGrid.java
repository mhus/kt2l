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

package de.mhus.kt2l.resources.pods;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import de.mhus.commons.lang.IRegistration;
import de.mhus.commons.tools.MCast;
import de.mhus.commons.tools.MCollection;
import de.mhus.commons.tools.MLang;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.core.UiUtil;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.AbstractGrid;
import de.mhus.kt2l.resources.ExecutionContext;
import io.kubernetes.client.Metrics;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.custom.ContainerMetrics;
import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Watch;
import io.vavr.control.Try;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class PodGrid extends AbstractGrid<PodGrid.Pod,Grid<PodGrid.Container>> {

    public enum CONTAINER_TYPE {DEFAULT, INIT, EPHEMERAL};
    private List<Container> containerList = null;
    private Pod containerSelectedPod;
    private IRegistration podEventRegistration;

    protected void createDetailsComponent() {
        detailsComponent = new Grid<>(Container.class, false);
        addClassNames("contact-grid");
        detailsComponent.setWidthFull();
        detailsComponent.setHeight("200px");
        detailsComponent.addColumn(cont -> cont.getName()).setHeader("Name").setSortProperty("name");
        detailsComponent.addColumn(cont -> cont.getType()).setHeader("Type").setSortProperty("type");
        detailsComponent.addColumn(cont -> cont.getRestarts()).setHeader("Restarts").setSortProperty("restarts");
        detailsComponent.addColumn(cont -> cont.getStatus()).setHeader("Status").setSortProperty("status");
        detailsComponent.addColumn(cont -> cont.getAge()).setHeader("Age").setSortProperty("age");
        detailsComponent.addColumn(cont -> cont.getMetricCpuString()).setHeader("CPU").setSortProperty("cpu");
        detailsComponent.addColumn(cont -> cont.getMetricMemoryString()).setHeader("Mem").setSortProperty("memory");
        detailsComponent.getColumns().forEach(col -> {
            col.setAutoWidth(true);
            col.setResizable(true);
        });
        detailsComponent.setDataProvider(new ContainerProvider());
        detailsComponent.setVisible(false);
        detailsComponent.addSelectionListener(event -> {
            if (detailsComponent.isVisible()) {
                final var selected = detailsComponent.getSelectedItems().stream().map(s -> new PodGrid.Pod(s.getPod())).collect(Collectors.toSet());
                actions.forEach(a -> a.updateWithResources(selected));
            }
        });

        GridContextMenu<Container> menu = detailsComponent.addContextMenu();
        actions.stream().sorted(Comparator.comparingInt((MenuAction a) -> a.getAction().getMenuOrder())).forEach(action -> {
            // shortcut
            if (action.getAction().getShortcutKey() != null) {
                var shortcut = UiUtil.createShortcut(action.getAction().getShortcutKey());
                if (shortcut != null) {
                    shortcut.addShortcutListener(detailsComponent, () -> {
                        action.execute();
                    });
                }
            }

            // context menu item
            var item = createContextMenuItem(menu, action.getAction());
            item.addMenuItemClickListener(event -> {
                        var selected = detailsComponent.getSelectedItems().stream().map(c -> new ContainerResource(c)).collect(Collectors.toSet());
                        if (!action.getAction().canHandleResource(K8sUtil.RESOURCE_CONTAINER, selected)) {
                            Notification notification = Notification
                                    .show("Can't execute");
                            notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
                            return;
                        }

                        var context = ExecutionContext.builder()
                                .resourceType(K8sUtil.RESOURCE_CONTAINER)
                                .selected(selected)
                                .namespace(namespace)
                                .api(coreApi)
                                .clusterConfiguration(clusterConfig)
                                .ui(UI.getCurrent())
                                .grid(PodGrid.this)
                                .mainView(view.getMainView())
                                .selectedTab(view.getXTab())
                                .build();

                        action.execute(context);
                    }
            );
        });

    }

    @Override
    protected void onDetailsChanged(Pod item) {
        setContainerPod(item);
    }

    @Override
    protected void onShowDetails(Pod item, boolean flip) {
        flipContainerVisibility(item, flip, !flip);
    }

    private void flipContainerVisibility(Pod pod, boolean flip, boolean focus) {
        containerList = null;
        containerSelectedPod = null;
        detailsComponent.setVisible(!flip || !detailsComponent.isVisible());
        if (detailsComponent.isVisible()) {
            containerSelectedPod = pod;
            detailsComponent.getDataProvider().refreshAll();
            if (focus)
                detailsComponent.getElement().getNode()
                        .runWhenAttached(ui -> ui.getPage().executeJs(
                                "setTimeout(function(){let firstTd = $0.shadowRoot.querySelector('tr:first-child > td:first-child'); firstTd.click(); firstTd.focus(); },0)", detailsComponent.getElement()));
        }

    }


    @Override
    protected void onGridSelectionChanged() {
        if (detailsComponent.isVisible())
            detailsComponent.deselectAll();
    }

    @Override
    protected Class<Pod> getManagedClass() {
        return Pod.class;
    }

    protected void onGridCellFocusChanged(PodGrid.Pod item) {
        if (detailsComponent.isVisible()) {
            containerSelectedPod = item;
            containerList = null;
            detailsComponent.getDataProvider().refreshAll();
        }
    }

    @Override
    protected DataProvider createDataProvider() {
        return new PodProvider();
    }

    @Override
    protected boolean filterByRegex(Pod pod, String f) {
        return pod.getName().matches(f);
    }

    @Override
    protected KubernetesObject getSelectedKubernetesObject(Pod resource) {
        return resource.getPod();
    }

    public void refresh(long counter) {
        if (counter % 10 != 0) return;

        if (filteredList == null) return;

        Map<String, PodMetrics> metricMap = new HashMap<>();
        for (String ns : getKnownNamespaces()) {
            getNamespaceMetrics(ns).forEach(
                    metric -> metricMap.put(metric.getMetadata().getNamespace() + ":" + metric.getMetadata().getName(), metric)
            );
        }
        final AtomicBoolean changed = new AtomicBoolean(false);
        filteredList.stream().forEach(pod -> {
            var metric = metricMap.get(pod.getNamespace() + ":" + pod.getName());
            if (metric != null) {
                if (pod.updateMetric(metric)) {
                    resourcesGrid.getDataProvider().refreshItem(pod);
                    changed.set(true);
                }
            }
        } );
        if (MCollection.isSet(containerList)) {
            var v1Pod = containerList.getFirst().getPod();
            var metric = metricMap.get(v1Pod.getMetadata().getNamespace() + ":" + v1Pod.getMetadata().getName());
            if (metric != null) {
                final Map<String, ContainerMetrics> containerMetricMap = new HashMap<>();
                metric.getContainers().stream().forEach(m -> containerMetricMap.put(m.getName(), m));
                containerList.stream().forEach(container -> {
                    var containerMetric = containerMetricMap.get(container.getName());
                    if (containerMetric != null) {
                        if (container.updateMetric(containerMetric)) {
                            detailsComponent.getDataProvider().refreshItem(container);
                            changed.set(true);
                        }
                    }
                });
            }
        }

        if (changed.get()) {
            UI.getCurrent().push();
        }
    }

    private List<PodMetrics> getNamespaceMetrics(String ns) {
        Metrics metrics = new Metrics(coreApi.getApiClient());
        try {
            var list = metrics.getPodMetrics(ns);
            return list.getItems();
        } catch (Exception e) {
            LOGGER.error("Can't get metrics for namespace {}",ns,e);
        }
        return Collections.EMPTY_LIST;
    }

    private Set<String> getKnownNamespaces() {
        return filteredList.stream().map(pod -> pod.getNamespace()).collect(Collectors.toSet());
    }

    @Override
    public void destroy() {
        podEventRegistration.unregister();
    }

    @Override
    protected void createGridColumns(Grid<Pod> podGrid) {
        podGrid.addColumn(pod -> pod.getNamespace()).setHeader("Namespace").setSortProperty("namespace");
        podGrid.addColumn(pod -> pod.getName()).setHeader("Name").setSortProperty("name");
        podGrid.addColumn(pod -> pod.getReadyContainers()).setHeader("Ready").setSortProperty("ready");
        podGrid.addColumn(pod -> pod.getRestarts()).setHeader("Restarts").setSortProperty("restarts");
        podGrid.addColumn(pod -> pod.getStatus()).setHeader("Status").setSortProperty("status");
        podGrid.addColumn(pod -> pod.getAge()).setHeader("Age").setSortProperty("age");
        podGrid.addColumn(pod -> pod.getMetricCpuString()).setHeader("CPU").setSortProperty("cpu");
        podGrid.addColumn(pod -> pod.getMetricMemoryString()).setHeader("Mem").setSortProperty("memory");
    }

    @Override
    protected boolean filterByContent(Pod pod, String filter) {
        return pod.getName().contains(filter);
    }

    private void podEvent(Watch.Response<V1Pod> event) {
        if (resourcesList == null) return;
        if (namespace != null && !namespace.equals(K8sUtil.NAMESPACE_ALL) && !namespace.equals(event.object.getMetadata().getNamespace())) return;

        if (event.type.equals(K8sUtil.WATCH_EVENT_ADDED) || event.type.equals(K8sUtil.WATCH_EVENT_MODIFIED)) {

            final var foundPod = resourcesList.stream().filter(pod -> pod.getName().equals(event.object.getMetadata().getName())).findFirst().orElseGet(
                    () -> {
                        final var pod = new Pod(event.object);
                        resourcesList.add(pod);
                        return pod;
                    }
            );

            foundPod.setStatus(event.object.getStatus().getPhase());
            foundPod.setPod(event.object);
            filterList();
            resourcesGrid.getDataProvider().refreshItem(foundPod);
        }

        if (event.type.equals(K8sUtil.WATCH_EVENT_DELETED)) {
            resourcesList.forEach(pod -> {
                if (pod.getName().equals(event.object.getMetadata().getName())) {
                    resourcesList.remove(pod);
                    filterList();
                    resourcesGrid.getDataProvider().refreshAll();
                }
            });
        }

    }

    @Override
    protected void init() {
        podEventRegistration = view.getMainView().getBackgroundJob(clusterConfig.name(), ClusterPodWatch.class, () -> new ClusterPodWatch()).getEventHandler().registerWeak(this::podEvent);
    }

    @Override
    public String getManagedResourceType() {
        return K8sUtil.RESOURCE_PODS;
    }

    private class ContainerProvider extends CallbackDataProvider<Container, Void> {
        public ContainerProvider() {
            super(query -> {
                        LOGGER.debug("Cont: Do the query {}",query);
                        for(QuerySortOrder queryOrder :
                                query.getSortOrders()) {
                            Collections.sort(containerList, (a, b) -> switch (queryOrder.getSorted()) {
                                case "name" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> a.getName().compareTo(b.getName());
                                    case DESCENDING -> b.getName().compareTo(a.getName());
                                };
                                case "status" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> a.getStatus().compareTo(b.getStatus());
                                    case DESCENDING -> b.getStatus().compareTo(a.getStatus());
                                };
                                case "restarts" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> Long.compare(a.getRestarts(), b.getRestarts());
                                    case DESCENDING -> Long.compare(b.getRestarts(), a.getRestarts());
                                };
                                case "age" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> Long.compare(a.getCreated(), b.getCreated());
                                    case DESCENDING -> Long.compare(b.getCreated(), a.getCreated());
                                };
                                case "cpu" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> Double.compare(a.getMetricCpu(), b.getMetricCpu());
                                    case DESCENDING -> Double.compare(b.getMetricCpu(), a.getMetricCpu());
                                };
                                case "memory" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> Long.compare(a.getMetricMemory(), b.getMetricMemory());
                                    case DESCENDING -> Long.compare(b.getMetricMemory(), a.getMetricMemory());
                                };
                                default -> 0;
                            });

                        }
                        return containerList.stream().skip(query.getOffset()).limit(query.getLimit());
                    }, query -> {
                        LOGGER.debug("Do the size query {}",query);
                        if (containerList == null) {
                            containerList = new ArrayList<>();

                            final var selectedPod = containerSelectedPod;
                            if (selectedPod == null) return 0;
                            try {
                                selectedPod.getPod().getStatus().getContainerStatuses().forEach(
                                        cs -> {
                                            var container = new Container(
                                                    CONTAINER_TYPE.DEFAULT,
                                                    cs,
                                                    selectedPod.getPod()
                                            );
                                            var metric = selectedPod.getMetric();
                                            if (metric != null) {
                                                metric.getContainers().stream().filter(m -> m.getName().equals(container.getName())).findFirst().ifPresent(
                                                        m -> container.updateMetric(m)
                                                );
                                            }
                                            containerList.add(container);
                                        }
                                );
                                if (selectedPod.getPod().getStatus().getEphemeralContainerStatuses() != null)
                                    selectedPod.getPod().getStatus().getEphemeralContainerStatuses().forEach(
                                            cs -> {
                                                containerList.add(new Container(
                                                        CONTAINER_TYPE.EPHEMERAL,
                                                        cs,
                                                        selectedPod.getPod()
                                                ));
                                            }
                                    );
                                if (selectedPod.getPod().getStatus().getInitContainerStatuses() != null)
                                    selectedPod.getPod().getStatus().getInitContainerStatuses().forEach(
                                            cs -> {
                                                containerList.add(new Container(
                                                        CONTAINER_TYPE.INIT,
                                                        cs,
                                                        selectedPod.getPod()
                                                ));
                                            }
                                    );
                            } catch (Exception e) {
                                LOGGER.error("Can't fetch containers",e);
                            }
                        }
                        return containerList.size();
                    }
            );
        }
    }

    private class PodProvider extends CallbackDataProvider<Pod, Void> {

        public PodProvider() {
            super(query -> {
                        LOGGER.debug("Do the pods query {}",query);
                        if (filteredList == null) return Stream.empty();
                        for(QuerySortOrder queryOrder :
                                query.getSortOrders()) {
                            Collections.sort(filteredList, (a, b) -> switch (queryOrder.getSorted()) {
                                case "name" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> a.getName().compareTo(b.getName());
                                    case DESCENDING -> b.getName().compareTo(a.getName());
                                };
                                case "status" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> a.getStatus().compareTo(b.getStatus());
                                    case DESCENDING -> b.getStatus().compareTo(a.getStatus());
                                };
                                case "age" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> Long.compare(a.getCreated(), b.getCreated());
                                    case DESCENDING -> Long.compare(b.getCreated(), a.getCreated());
                                };
                                case "namespace" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> a.getNamespace().compareTo(b.getNamespace());
                                    case DESCENDING -> b.getNamespace().compareTo(a.getNamespace());
                                };
                                case "ready" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> Long.compare(a.getRunningContainersCnt(), b.getRunningContainersCnt());
                                    case DESCENDING -> Long.compare(b.getRunningContainersCnt(), a.getRunningContainersCnt());
                                };
                                case "restarts" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> Long.compare(a.getRestarts(), b.getRestarts());
                                    case DESCENDING -> Long.compare(b.getRestarts(), a.getRestarts());
                                };
                                case "cpu" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> Double.compare(a.getMetricCpu(), b.getMetricCpu());
                                    case DESCENDING -> Double.compare(b.getMetricCpu(), a.getMetricCpu());
                                };
                                case "memory" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> Long.compare(a.getMetricMemory(), b.getMetricMemory());
                                    case DESCENDING -> Long.compare(b.getMetricMemory(), a.getMetricMemory());
                                };
                                default -> 0;
                            });

                        }
                        return filteredList.stream().skip(query.getOffset()).limit(query.getLimit());
                    }, query -> {
                        LOGGER.debug("Do the size query {}",query);
                        if (resourcesList == null) {
                            resourcesList = new ArrayList<>();
                            final var namespaceName = namespace ==  null || namespace.equals(K8sUtil.NAMESPACE_ALL) ? null : (String) namespace;
                            Try.of(() -> namespaceName == null ? coreApi.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null ) :  coreApi.listNamespacedPod(namespaceName, null, null, null, null, null, null, null, null, null, null))
                                    .onFailure(e -> LOGGER.error("Can't fetch pods from cluster",e))
                                    .onSuccess(podList -> {
                                        podList.getItems().forEach(pod ->
                                            PodGrid.this.resourcesList.add(new Pod(pod))
                                        );
                                    });
                        }
                        filterList();
                        return filteredList.size();
                    }
            );
        }

    }

    @Data
    public static class Pod {
        private String name;
        private String namespace;
        private String status;
        private long runningContainersCnt;
        private long containerCnt;
        private long created;
        private long restarts;

        private double metricCpu = Double.MAX_VALUE;
        private long metricMemory = Long.MAX_VALUE;

        private String metricCpuString = "-";
        private String metricMemoryString = "-";

        private V1Pod pod;
        private PodMetrics metric;

        public Pod(V1Pod pod) {
            this.pod = pod;
            this.name = pod.getMetadata().getName();
            this.namespace = pod.getMetadata().getNamespace();
            this.created = pod.getMetadata().getCreationTimestamp().toEpochSecond();
            this.status = pod.getStatus().getPhase();
            var containers = pod.getStatus().getContainerStatuses();
            this.containerCnt = containers.size();
            this.runningContainersCnt = containers.stream().filter(c -> c.getState().getRunning() != null).count();
            for (V1ContainerStatus container : containers) {
                this.restarts += container.getRestartCount();
            }
        }

        public String getAge() {
            return K8sUtil.getAge(pod.getMetadata().getCreationTimestamp());
        }

        public String getReadyContainers() {
            return runningContainersCnt + "/" + containerCnt;
        }

        public boolean updateMetric(PodMetrics metric) {
            this.metric = metric;

            double cpu = 0;
            long mem = 0;
            for (ContainerMetrics container : metric.getContainers()) {
                cpu += container.getUsage().get("cpu").getNumber().doubleValue();
                mem += container.getUsage().get("memory").getNumber().longValue();
            }
            var cpuString = MString.truncate(String.valueOf(cpu), 5);
            var memoryString = MCast.toByteUnit(mem);
            if (cpuString.equals(metricCpuString) && memoryString.equals(metricMemoryString)) return false;
            metricCpuString = cpuString;
            metricMemoryString = memoryString;
            metricCpu = cpu;
            metricMemory = mem;
            return true;
        }

        public boolean equals(Object obj) {
            if (obj instanceof Pod other) {
                return MLang.tryThis(() -> other.getName().equals(name) && other.getNamespace().equals(namespace)).or(false);
            }
            return false;
        }

    }

    @Data
    public static final class Container {
        private final String name;
        private final String namespace;
        private final String status;
        private final CONTAINER_TYPE type;
        private String age = "-";
        private long created = 0;
        private long restarts = 0;
        private final V1Pod pod;
        private double metricCpu = Double.MAX_VALUE;
        private long metricMemory = Long.MAX_VALUE;
        private String metricCpuString = "-";
        private String metricMemoryString = "-";

        public Container(
                CONTAINER_TYPE type,
                V1ContainerStatus cs,
                V1Pod pod
        ) {
            this.name = cs.getName();
            this.namespace = pod.getMetadata().getNamespace();
            if (cs.getState().getTerminated() != null) {
                this.status = "Terminated";
            } else if (cs.getState().getRunning() != null) {
                this.status = "Running";
                this.age = K8sUtil.getAge(cs.getState().getRunning().getStartedAt());
                this.created = cs.getState().getRunning().getStartedAt().toEpochSecond();
            } else if (cs.getState().getWaiting() != null) {
                this.status = "Waiting";
            } else {
                this.status = "Unknown";
            }
            this.type = type;
            this.restarts = cs.getRestartCount();
            this.pod = pod;
        }

        public boolean updateMetric(ContainerMetrics containerMetric) {
            var cpu = containerMetric.getUsage().get("cpu").getNumber().doubleValue();
            var mem = containerMetric.getUsage().get("memory").getNumber().longValue();
            var cpuString = MString.truncate(String.valueOf(cpu), 5);
            var memoryString = MCast.toByteUnit(mem);
            if (cpuString.equals(metricCpuString) && memoryString.equals(metricMemoryString)) return false;
            metricCpuString = cpuString;
            metricMemoryString = memoryString;
            metricCpu = cpu;
            metricMemory = mem;
            return true;
        }

        public boolean equals(Object obj) {
            if (obj instanceof Container other) {
                return MLang.tryThis(() -> other.getName().equals(name) &&
                        other.getPod().getMetadata().getName().equals(pod.getMetadata().getName()) &&
                        other.getNamespace().equals(namespace)
                    ).or(false);
            }
            return false;
        }

    }

    private void setContainerPod(Pod item) {
        onGridCellFocusChanged(item);
    }

}


