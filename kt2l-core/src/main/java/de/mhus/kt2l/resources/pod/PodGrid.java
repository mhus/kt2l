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

package de.mhus.kt2l.resources.pod;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.commons.tools.MCast;
import de.mhus.commons.tools.MCollection;
import de.mhus.commons.tools.MLang;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceFilterFactory;
import de.mhus.kt2l.resources.pod.score.PodScorer;
import de.mhus.kt2l.resources.pod.score.PodScorerConfiguration;
import de.mhus.kt2l.resources.util.AbstractGridWithNamespace;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.Metrics;
import io.kubernetes.client.custom.ContainerMetrics;
import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Configurable
@Slf4j
public class PodGrid extends AbstractGridWithNamespace<PodGrid.Resource,Grid<PodGrid.Container>, V1Pod, V1PodList> {

    @Autowired(required = false)
    private List<PodScorer> podSourcerers;

    @Autowired
    private PodScorerConfiguration podScorerConfiguration;

    private int scoreErrorThreshold;
    private int scoreWarnThreshold;
    private boolean scoringEnabled;
    private long disableMetricsUntil;

    public enum CONTAINER_TYPE {DEFAULT, INIT, EPHEMERAL};
      private List<Container> containerList = null;
      private Resource containerSelectedPod;

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return PodWatch.class;
    }

    @Override
    protected Resource createResourceItem() {
        return new Resource(this);
    }

    protected void createDetailsComponent() {
        detailsComponent = new Grid<>(Container.class, false);
        addClassNames("contact-grid");
        detailsComponent.setWidthFull();
        detailsComponent.setHeight("200px");
        detailsComponent.addColumn(Container::getName).setHeader("Name").setSortProperty("name");
        detailsComponent.addColumn(Container::getType).setHeader("Type").setSortProperty("type");
        detailsComponent.addColumn(Container::getRestarts).setHeader("Restarts").setSortProperty("restarts");
        detailsComponent.addColumn(Container::getStatus).setHeader("Status").setSortProperty("status");
        detailsComponent.addColumn(Container::getAge).setHeader("Age").setSortProperty("age");
        if (cluster.isMetricsEnabled()) {
            detailsComponent.addColumn(Container::getMetricCpuString).setHeader("CPU").setSortProperty("cpu");
            detailsComponent.addColumn(Container::getMetricMemoryString).setHeader("Mem").setSortProperty("memory");
        }
        detailsComponent.getColumns().forEach(col -> {
            col.setAutoWidth(true);
            col.setResizable(true);
        });
        detailsComponent.setDataProvider(new ContainerProvider());
        detailsComponent.setVisible(false);
        detailsComponent.addSelectionListener(event -> {
            if (detailsComponent.isVisible()) {
                final var selected = detailsComponent.getSelectedItems().stream().map(s -> new Resource(this, s.getPod())).collect(Collectors.toSet());
                actions.forEach(a -> a.updateWithResources(selected));
            }
        });

        GridContextMenu<Container> menu = detailsComponent.addContextMenu();
        actions.stream().sorted(Comparator.comparingInt((MenuAction a) -> a.getAction().getMenuOrder())).forEach(action -> {
            // shortcut
            if (action.getAction().getShortcutKey() != null) {
                var shortcut = UiUtil.createShortcut(action.getAction().getShortcutKey());
                if (shortcut != null) {
                    shortcut.addShortcutListener(detailsComponent, action::execute);
                }
            }

            // context menu item
            var item = createContextMenuItem(menu, action.getAction());
            item.addMenuItemClickListener(event -> {
                        var selected = detailsComponent.getSelectedItems().stream().map(ContainerResource::new).collect(Collectors.toSet());
                        if (!action.getAction().canHandleResource(cluster, K8s.CONTAINER, selected)) {
                            Notification notification = Notification
                                    .show("Can't execute");
                            notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
                            return;
                        }

                        var context = ExecutionContext.builder()
                                .type(K8s.CONTAINER)
                                .selected(selected)
                                .namespace(namespace)
                                .cluster(cluster)
                                .ui(getPanel().getCore().ui())
                                .grid(PodGrid.this)
                                .core(panel.getCore())
                                .selectedTab(panel.getTab())
                                .build();

                        action.execute(context);
                    }
            );
        });

    }

    @Override
    protected void onDetailsChanged(Resource item) {
        onGridCellFocusChanged(item);
    }

    @Override
    protected void onShowDetails(Resource item, boolean flip) {
        containerList = null;
        containerSelectedPod = null;
        detailsComponent.setVisible(!flip || !detailsComponent.isVisible());
        if (detailsComponent.isVisible()) {
            containerSelectedPod = item;
            detailsComponent.getDataProvider().refreshAll();
            if (!flip)
                detailsComponent.getElement().getNode()
                        .runWhenAttached(ui -> getPanel().getCore().ui().getPage().executeJs(
                                "setTimeout(function(){let firstTd = $0.shadowRoot.querySelector('tr:first-child > td:first-child'); firstTd.click(); firstTd.focus(); },0)", detailsComponent.getElement()));
        }

    }

    @Override
    protected void onGridSelectionChanged() {
        if (detailsComponent.isVisible())
            detailsComponent.deselectAll();
    }

    @Override
    protected void onGridCellFocusChanged(Resource item) {
        if (detailsComponent.isVisible()) {
            containerSelectedPod = item;
            containerList = null;
            detailsComponent.getDataProvider().refreshAll();
        }
    }

    @Override
    protected void doRefreshGrid() {
        super.doRefreshGrid();
    }

    @Override
    public void refresh(long counter) {
        super.refresh(counter);
        if (counter % 10 == 2)
            updateMetrics();
    }

    protected synchronized void updateMetrics() {
        if (filteredList == null) return;
        if (!cluster.isMetricsEnabled()) return;

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
                if (scoringEnabled && podSourcerers != null) {
                    var score = 0;
                    for (PodScorer scorer : podSourcerers) {
                        var apiProvider = panel.getCluster().getApiProvider();
                        if (scorer.isEnabled())
                            score += scorer.scorePod(cluster, apiProvider, pod);
                    }
                    if (score != pod.score) {
                        changed.set(true);
                        pod.score = score;

                        if (score > scoreErrorThreshold) {
                            pod.alert = ALERT.ALERT;
                        } else if (score > scoreWarnThreshold) {
                            pod.alert = ALERT.WARNING;
                        } else {
                            pod.alert = ALERT.NONE;
                        }

                    }
                }
            }
        });
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
            getPanel().getCore().ui().push();
        }
    }

    @SuppressWarnings("unchecked")
    private List<PodMetrics> getNamespaceMetrics(String ns) {
        if (disableMetricsUntil != 0 && disableMetricsUntil > System.currentTimeMillis()) 
            return Collections.EMPTY_LIST;
        Metrics metrics = new Metrics(panel.getCluster().getApiProvider().getClient());
        try {
            var list = metrics.getPodMetrics(ns);
            return list.getItems();
        } catch (ApiException e) {
            LOGGER.error("Can't get metrics for namespace {} with RC {}",ns, e.getCode(), e);
            panel.getCluster().getApiProvider().invalidate();
            disableMetricsUntil = System.currentTimeMillis() + 60000;
        } catch (Exception e) {
            LOGGER.error("Can't get metrics for namespace {}",ns,e);
        }
        return Collections.EMPTY_LIST;
    }

    private Set<String> getKnownNamespaces() {
        return filteredList.stream().map(ResourceItem::getNamespace).collect(Collectors.toSet());
    }

    @Override
    public V1APIResource getManagedType() {
        return K8s.POD;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> podGrid) {
        scoreErrorThreshold = podScorerConfiguration.getErrorThreshold();
        scoreWarnThreshold = podScorerConfiguration.getWarnThreshold();
        scoringEnabled = podScorerConfiguration.isEnabled();

        podGrid.addColumn(Resource::getReadyContainers).setHeader("Ready").setSortProperty("ready");
        podGrid.addColumn(Resource::getRestarts).setHeader("Restarts").setSortProperty("restarts");
        podGrid.addColumn(Resource::getStatus).setHeader("Status").setSortProperty("status");
        if (cluster.isMetricsEnabled()) {
            podGrid.addColumn(Resource::getMetricCpuString).setHeader("CPU").setSortProperty("cpu");
            podGrid.addColumn(pod -> pod.getMetricCpuPercentage() < 0 ? "" : pod.getMetricCpuPercentage()).setHeader("CPU%").setSortProperty("cpu%");
            podGrid.addColumn(Resource::getMetricMemoryString).setHeader("Mem").setSortProperty("memory");
            podGrid.addColumn(pod -> pod.getMetricMemoryPercentage() < 0 ? "" : pod.getMetricMemoryPercentage()).setHeader("Mem%").setSortProperty("memory%");
        }
        if (scoringEnabled)
            podGrid.addComponentColumn(this::createScoreColumnComponent).setHeader("Score").setSortProperty("score");
    }

    private Component createScoreColumnComponent(Resource pod) {
        var label = new Div();
        if (pod.getScore() >= 0) {
            Icon icon;
            if (pod.getScore() > scoreErrorThreshold)
                icon = VaadinIcon.FIRE.create();
            else if (pod.getScore() > scoreWarnThreshold)
                icon = VaadinIcon.WARNING.create();
            else
                icon = VaadinIcon.CHECK.create();
            icon.getStyle().set("width", "var(--lumo-icon-size-s)");
            icon.getStyle().set("height", "var(--lumo-icon-size-s)");
            icon.getStyle().set("marginRight", "var(--lumo-space-s)");
            label.add(icon);
            label.add(new Text(String.valueOf(pod.getScore())));
        }
        return label;
    }

    protected void createGridColumnsAtEnd(Grid<Resource> podGrid) {
        podGrid.addColumn(pod -> pod.getContainerAge()).setHeader("CAge").setSortProperty("cage");
        podGrid.addColumn(pod -> pod.getNode()).setHeader("Node").setSortProperty("node");
        podGrid.addColumn(pod -> pod.getIp()).setHeader("IP").setSortProperty("ip");
        podGrid.addColumn(pod -> pod.getOwner()).setHeader("Owner").setSortProperty("owner");
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        return switch (sorted) {
            case "status" -> switch (direction) {
                case ASCENDING -> a.getStatus().compareTo(b.getStatus());
                case DESCENDING -> b.getStatus().compareTo(a.getStatus());
            };
            case "ready" -> switch (direction) {
                case ASCENDING -> Long.compare(a.getRunningContainersCnt(), b.getRunningContainersCnt());
                case DESCENDING -> Long.compare(b.getRunningContainersCnt(), a.getRunningContainersCnt());
            };
            case "restarts" -> switch (direction) {
                case ASCENDING -> Long.compare(a.getRestarts(), b.getRestarts());
                case DESCENDING -> Long.compare(b.getRestarts(), a.getRestarts());
            };
            case "cpu" -> switch (direction) {
                case ASCENDING -> Double.compare(a.getMetricCpu(), b.getMetricCpu());
                case DESCENDING -> Double.compare(b.getMetricCpu(), a.getMetricCpu());
            };
            case "cpu%" -> switch (direction) {
                case ASCENDING -> Integer.compare(a.getMetricCpuPercentage(), b.getMetricCpuPercentage());
                case DESCENDING -> Integer.compare(b.getMetricCpuPercentage(), a.getMetricCpuPercentage());
            };
            case "memory" -> switch (direction) {
                case ASCENDING -> Long.compare(a.getMetricMemory(), b.getMetricMemory());
                case DESCENDING -> Long.compare(b.getMetricMemory(), a.getMetricMemory());
            };
            case "memory%" -> switch (direction) {
                case ASCENDING -> Integer.compare(a.getMetricMemoryPercentage(), b.getMetricMemoryPercentage());
                case DESCENDING -> Integer.compare(b.getMetricMemoryPercentage(), a.getMetricMemoryPercentage());
            };
            case "score" -> switch (direction) {
                case ASCENDING -> Integer.compare(a.getScore(), b.getScore());
                case DESCENDING -> Integer.compare(b.getScore(), a.getScore());
            };
            case "owner" -> switch (direction) {
                case ASCENDING -> a.getOwner().compareTo(b.getOwner());
                case DESCENDING -> b.getOwner().compareTo(a.getOwner());
            };
            case "node" -> switch (direction) {
                case ASCENDING -> a.getNode().compareTo(b.getNode());
                case DESCENDING -> b.getNode().compareTo(a.getNode());
            };
            case "ip" -> switch (direction) {
                case ASCENDING -> a.getIp().compareTo(b.getIp());
                case DESCENDING -> b.getIp().compareTo(a.getIp());
            };
            case "cage" -> switch (direction) {
                case ASCENDING -> K8sUtil.compareTo(a.getContainerCreated(), b.getContainerCreated());
                case DESCENDING -> K8sUtil.compareTo(b.getContainerCreated(), a.getContainerCreated());
            };
            default -> 0;
        };
    }

    private class ContainerProvider extends CallbackDataProvider<Container, Void> {
        public ContainerProvider() {
            super(query -> {
                        LOGGER.debug("◌ Cont: Do the query {}", queryToString(query));
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
                                    case ASCENDING -> K8sUtil.compareTo(a.getCreated(), b.getCreated());
                                    case DESCENDING -> K8sUtil.compareTo(b.getCreated(), a.getCreated());
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
                        LOGGER.debug("◌ Cont: Do the size query {}", queryToString(query));
                        if (containerList == null) {
                            containerList = new ArrayList<>();

                            final var selectedPod = containerSelectedPod;
                            if (selectedPod == null) return 0;
                            try {
                                selectedPod.getResource().getStatus().getContainerStatuses().forEach(
                                        cs -> {
                                            var container = new Container(
                                                    CONTAINER_TYPE.DEFAULT,
                                                    cs,
                                                    selectedPod.getResource()
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
                                if (selectedPod.getResource().getStatus().getEphemeralContainerStatuses() != null)
                                    selectedPod.getResource().getStatus().getEphemeralContainerStatuses().forEach(
                                            cs -> {
                                                containerList.add(new Container(
                                                        CONTAINER_TYPE.EPHEMERAL,
                                                        cs,
                                                        selectedPod.getResource()
                                                ));
                                            }
                                    );
                                if (selectedPod.getResource().getStatus().getInitContainerStatuses() != null)
                                    selectedPod.getResource().getStatus().getInitContainerStatuses().forEach(
                                            cs -> {
                                                containerList.add(new Container(
                                                        CONTAINER_TYPE.INIT,
                                                        cs,
                                                        selectedPod.getResource()
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

    @Getter
    public static class Resource extends AbstractGridWithNamespace.ResourceItem<V1Pod> {

        private final PodGrid grid;
        private String status;
        private int runningContainersCnt;
        private int containerCnt;
        private int allContainerCnt;
        private int restarts;

        private double metricCpu = Double.MAX_VALUE;
        private long metricMemory = Long.MAX_VALUE;

        private String metricCpuString = "-";
        private String metricMemoryString = "-";

        private int metricCpuPercentage = -1;
        private int metricMemoryPercentage = -1;

        private int score = -1;

        private String owner = "";
        private String node = "";
        private String ip = "";

        protected OffsetDateTime containerCreated;

        private PodMetrics metric;

        public Resource(PodGrid grid) {
            super();
            this.grid = grid;
        }

        public Resource(PodGrid grid, V1Pod pod) {
            this.grid = grid;
            resource = pod;
            updateResource();
        }

        public void updateResource() {
            super.updateResource();

            var ownerReferences = resource.getMetadata().getOwnerReferences();
            if (ownerReferences != null && !ownerReferences.isEmpty()) {
                var ownerReference = ownerReferences.get(0);
                owner = ownerReference.getKind() + "-" + ownerReference.getUid();
            }
            node = resource.getSpec().getNodeName();
            ip = resource.getStatus().getPodIP();

            if (this.status != null && !Objects.equals(this.status, resource.getStatus().getPhase())) {
                setFlashColor(UiUtil.COLOR.MAGENTA);
            }
            this.status = resource.getStatus().getPhase();
            if ("Succeeded".equals(this.status)) {
                // reset metrics if pod is done
                metricCpuString = "-";
                metricMemoryString = "-";
                metricCpuPercentage = -1;
                metricMemoryPercentage = -1;
                metricCpu = Double.MAX_VALUE;
                metricMemory = Long.MAX_VALUE;
            }
            this.restarts = 0;
            this.runningContainersCnt = 0;
            this.containerCnt = 0;
            this.allContainerCnt = 0;
            this.containerCreated = null;
            {
                var containers = resource.getStatus().getContainerStatuses();
                if (containers != null) {
                    for (V1ContainerStatus container : containers) {
                        this.containerCnt++;
                        this.allContainerCnt++;
                        if (container.getReady() != null && container.getReady())
                            this.runningContainersCnt++;
                        this.restarts += container.getRestartCount();
                        var created = MLang.tryThis(() -> container.getState().getRunning().getStartedAt()).orElse(null);
                        if (created !=  null && (containerCreated == null || created.isAfter(containerCreated)))
                            containerCreated = created;
                    }
                }
            }
            {
                var containers = resource.getStatus().getEphemeralContainerStatuses();
                if (containers != null) {
                    for (V1ContainerStatus container : containers) {
                        if (container.getState() != null && container.getState().getTerminated() == null) {
                            this.allContainerCnt++;
                            if (container.getReady() != null && container.getReady())
                                this.runningContainersCnt++;
                        }
                    }
                }
            }
            {
                var containers = resource.getStatus().getInitContainerStatuses();
                if (containers != null) {
                    for (V1ContainerStatus container : containers) {
                        if (container.getState() != null && container.getState().getTerminated() == null) {
                            this.allContainerCnt++;
                            if (container.getReady() != null && container.getReady())
                                this.runningContainersCnt++;
                        }
                    }
                }
            }

            if (resource.getMetadata().getDeletionTimestamp() != null) {
                this.status = "Terminating";
                setColor(UiUtil.COLOR.BROWN);
            } else
            if ("Succeeded".equals(this.status)) {
                setColor(UiUtil.COLOR.GREY);
            } else
            if (runningContainersCnt != containerCnt) {
                setColor(UiUtil.COLOR.RED);
            } else
                setColor(null);

            if (score > grid.scoreErrorThreshold) {
                alert = ALERT.ALERT;
            } else if (score > grid.scoreWarnThreshold) {
                alert = ALERT.WARNING;
            } else {
                alert = ALERT.NONE;
            }

        }

        public String getAge() {
            return K8sUtil.getAge(resource.getMetadata().getCreationTimestamp()) + (resource.getMetadata().getDeletionTimestamp() != null ? " (" + K8sUtil.getAge(resource.getMetadata().getDeletionTimestamp()) + ")" : "");
        }

        public String getReadyContainers() {
            return runningContainersCnt + "/" + containerCnt + ( containerCnt != allContainerCnt ? "/" + allContainerCnt : "");
        }

        public synchronized boolean updateMetric(PodMetrics metric) {
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

            metricCpuPercentage = -1;
            metricMemoryPercentage = -1;
            if (resource.getSpec().getContainers() != null) {
                double cpuLimit = 0;
                long memLimit = 0;
                for (V1Container container : resource.getSpec().getContainers()) {
                    if (container.getResources() != null && container.getResources().getLimits() != null) {
                        if (container.getResources().getLimits().get("cpu") != null)
                            cpuLimit += container.getResources().getLimits().get("cpu").getNumber().doubleValue();
                        if (container.getResources().getLimits().get("memory") != null)
                            memLimit += container.getResources().getLimits().get("memory").getNumber().longValue();
                    }
                }
                if (cpuLimit > 0)
                    metricCpuPercentage = (int) (cpu * 100 / cpuLimit);
                if (memLimit > 0)
                    metricMemoryPercentage = (int) (mem * 100 / memLimit);
            }

            return true;
        }

        public boolean equals(Object obj) {
            if (obj instanceof Resource other) {
                return MLang.tryThis(() -> other.getName().equals(name) && other.getNamespace().equals(namespace)).orElse(false);
            }
            if (obj instanceof V1Pod other) {
                return MLang.tryThis(() -> other.getMetadata().getName().equals(name) && other.getMetadata().getNamespace().equals(namespace)).orElse(false);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, namespace);
        }

        public String getContainerAge() {
            return containerCreated == null ? "-" : K8sUtil.getAge(containerCreated);
        }
    }

    @Data
    public static final class Container {
        private final String name;
        private final String namespace;
        private final String status;
        private final CONTAINER_TYPE type;
        private String age = "-";
        private OffsetDateTime created;
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
                this.created = cs.getState().getRunning().getStartedAt();
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
                    ).orElse(false);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, namespace);
        }
    }

    @Override
    public List<ResourceFilterFactory> getResourceFilterFactories() {
        return List.of(
                new ResourceFilterFactory("Terminating",res -> res.getMetadata().getDeletionTimestamp() != null),
                new ResourceFilterFactory("Running",res -> "Running".equalsIgnoreCase( ((V1Pod)res).getStatus().getPhase() ))
        );
    }

}


