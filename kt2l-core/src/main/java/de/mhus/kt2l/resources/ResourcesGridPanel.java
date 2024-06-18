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

package de.mhus.kt2l.resources;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.ThemableLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.mhus.commons.errors.NotFoundRuntimeException;
import de.mhus.commons.lang.IRegistration;
import de.mhus.commons.tools.MCollection;
import de.mhus.commons.tools.MLang;
import de.mhus.commons.tools.MObject;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterService;
import de.mhus.kt2l.config.AaaConfiguration;
import de.mhus.kt2l.config.ViewsConfiguration;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.core.SecurityContext;
import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.generic.GenericGrid;
import de.mhus.kt2l.resources.generic.GenericK8s;
import de.mhus.kt2l.resources.namespace.NamespaceWatch;
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.security.Principal;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static de.mhus.commons.tools.MString.isEmpty;

@Configurable
@Slf4j
public class ResourcesGridPanel extends VerticalLayout implements DeskTabListener {

    @Getter
    private final Core core;

    @Autowired
    @Getter
    private K8sService k8s;

    @Autowired
    @Getter
    private ClusterService clusterService;

    @Autowired
    private List<ResourceGridFactory> resourceGridFactories;

    @Getter
    private String clusterId;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private PanelService panelService;

    @Autowired
    private ViewsConfiguration viewConfig;

    @Getter
    private ResourcesGrid grid;
    private TextField filterText;
    private ComboBox<String> namespaceSelector;
    private ComboBox<V1APIResource> resourceSelector;
    @Getter
    private Cluster cluster;
    private VerticalLayout gridContainer;
    @Getter
    private K8s currentResourceType;
    @Getter
    private DeskTab tab;
    private ResourcesFilter resourcesFilter;
    private Button resourceFilterButton;
    private IRegistration namespaceEventRegistration;
    private Button historyBackButton;
    private Button historyForwardButton;
    private LinkedList<HistoryItem> history = new LinkedList<>();
    private int historyPointer = 0;
    private int historyMaxSize = 50;

    public ResourcesGridPanel(String clusterId, Core core) {
        this.clusterId = clusterId;
        this.core = core;
    }

    public void createUI() {

        resourceFilterButton = new Button(VaadinIcon.FILTER.create(), e -> {
            if (resourcesFilter != null) {
                resourcesFilter = null;
                grid.setFilter(filterText.getValue(), resourcesFilter);
                resourceFilterButton.setEnabled(false);
                resourceFilterButton.setTooltipText("");
                if (e.isFromClient())
                    historyAdd();
            }
        });
        resourceFilterButton.setEnabled(false);

        filterText = new TextField();
        namespaceSelector = new ComboBox<>();
        namespaceSelector.addFocusListener(e -> namespaceSelector.getElement().executeJs("this.querySelector(\"input\").select()") );
        resourceSelector = new ComboBox<>();
        resourceSelector.setRenderer(new ComponentRenderer<Component, V1APIResource>(item -> {
            Div div = new Div(item.getName() + (item.getShortNames() == null ? "" : " " + item.getShortNames()));
            if (currentResourceType != null && item.getName().equals(currentResourceType.resourceType()))
                div.addClassName("color-blue");
            else
            if (k8s.getResourceHandler(item) instanceof GenericK8s)
                div.addClassName("color-grey");
            return div;
        }));
        resourceSelector.addFocusListener(e -> resourceSelector.getElement().executeJs("this.querySelector(\"input\").select()") );

        addClassName("list-view");
        setSizeFull();

        gridContainer = new VerticalLayout();
        gridContainer.setSizeFull();
        gridContainer.setPadding(false);
        gridContainer.setSpacing(false);
        gridContainer.setMargin(false);

        setPadding(false);
        setMargin(false);

        add(getToolbar(), gridContainer);

    }

    private HorizontalLayout getToolbar() {

        // namespace selector
        namespaceSelector.setPlaceholder("Namespace");
        namespaceSelector.getStyle().set("--vaadin-combo-box-overlay-width", "350px");
        namespaceSelector.setItemLabelGenerator(String::toString);

        updateNamespaceSelector(true);
        namespaceSelector.addValueChangeListener(e -> {
            if (grid != null && !MString.equals(e.getValue(), grid.getNamespace())) {
                grid.setNamespace(e.getValue());
                if (e.isFromClient())
                    historyAdd();
            }
        });
        // resource type selector
        resourceSelector.setPlaceholder("Resource");
        resourceSelector.getStyle().set("--vaadin-combo-box-overlay-width", "350px");
        resourceSelector.setItemLabelGenerator((ItemLabelGenerator<V1APIResource>) item -> {
            var shortNames = item.getShortNames();
            var name = item.getSingularName();
            if (isEmpty(name)) name = item.getName();
            return name + (shortNames != null ? " " + shortNames : "");
        });
        resourceSelector.addValueChangeListener(e -> {
            resourceTypeChanged();
            if (e.isFromClient())
                historyAdd();
        });

        final Principal principal = securityService.getPrincipal();

        k8s.fillResourceTypes(cluster).handle((types, t) -> {
            if (t != null) {
                LOGGER.error("Can't fetch resource types",t);
                return Collections.emptyList();
            }
            LOGGER.debug("Resource types: {}",types.stream().map(V1APIResource::getName).toList());
            core.ui().access(() -> {
                resourceSelector.setItems(cluster.getResourceTypes().stream().sorted(Comparator.comparing(V1APIResource::getName)).toList());
                Thread.startVirtualThread(() -> {
                    MThread.sleep(400);
                    core.ui().access(() -> {
                        resourceSelector.setValue(
                                k8s.findResource(currentResourceType, cluster.getApiProvider(), principal));
                    });
                });
            });
            return types;
        });

        // filter text
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> {
            if (grid != null) {
                grid.setFilter(e.getValue(), resourcesFilter);
                if (e.isFromClient())
                    historyAdd();
            }
        });

        historyBackButton = new Button(VaadinIcon.ARROW_LEFT.create(), e -> {
            historyBack();
        });

        historyForwardButton = new Button(VaadinIcon.ARROW_RIGHT.create(), e -> {
            historyForward();
        });

        var cloneButton = new Button(VaadinIcon.COPY_O.create(), e -> clonePanel());
        cloneButton.setTooltipText("Duplicate resource panel");

        Div spacer = new Div();
        spacer.setWidthFull();

        // toolbar
        var toolbar = new HorizontalLayout(resourceFilterButton, filterText, namespaceSelector,resourceSelector, spacer, historyBackButton, historyForwardButton, cloneButton);
        toolbar.addClassName("toolbar");
        toolbar.setPadding(false);
        toolbar.setSpacing(false);
        toolbar.getThemeList().add("spacing-xs");
        toolbar.setMargin(false);
        toolbar.setWidthFull();
        historyUpdate();
        return toolbar;
    }

    private synchronized void historyUpdate() {
        historyBackButton.setEnabled(historyPointer > 1);
        historyForwardButton.setEnabled(historyPointer < history.size());
    }

    private synchronized void historyForward() {
        if (historyPointer < history.size()) {
            var item = history.get(historyPointer++);
            showResourcesInternal(item.resourceType, item.namespace, item.filter, item.filterText, item.sortOrder, item.sortAscending, history, historyPointer);
        }
        historyUpdate();
    }

    private synchronized void historyBack() {
        if (historyPointer > 1) {
            historyPointer--;
            var item = history.get(historyPointer-1);
            showResourcesInternal(item.resourceType, item.namespace, item.filter, item.filterText, item.sortOrder, item.sortAscending, history, historyPointer);
        }
        historyUpdate();
    }


    public synchronized void historyAdd() {
        if (historyPointer < history.size()) {
            history.subList(historyPointer, history.size()).clear();
        }
        var sortOrderList = grid.getSortOrder();
        String sortOrder = sortOrderList != null && sortOrderList.size() > 0 ? sortOrderList.get(0).getSorted().getKey() : null;
        boolean orderAsc = sortOrderList != null && sortOrderList.size() > 0 ? sortOrderList.get(0).getDirection().equals(SortDirection.ASCENDING) : true;

        historyAdd(currentResourceType, namespaceSelector.getValue(), resourcesFilter, filterText.getValue(), sortOrder, orderAsc);
    }

    protected synchronized void historyAdd(K8s resourceType, String namespace, ResourcesFilter filter, String filterText, String sortOrder, boolean sortAscending) {
        if (historyPointer < history.size()) {
            history.subList(historyPointer, history.size()).clear();
        }
        var entry = new HistoryItem(currentResourceType, namespaceSelector.getValue(), resourcesFilter, filterText, sortOrder, sortAscending);
        if (history.size() > 0 && history.getLast().equals(entry)) return;

        history.add(entry);
        if (history.size() > historyMaxSize)
            history.removeFirst();
        historyPointer = history.size();
        historyUpdate();
    }

    private void clonePanel() {
        var newTab = panelService.addResourcesGrid(tab, core, cluster);
        newTab.select();
        final var sc = SecurityContext.create();
        Thread.startVirtualThread(() -> {
            try (var cce = sc.enter()) {
                for (int i = 0; i < 10; i++) {
                    MThread.sleep(200);
                    if (((ResourcesGridPanel) newTab.getPanel()).getCurrentResourceType() != null) {
                        core.ui().access(() -> {
                            var sortOrderList = grid.getSortOrder();
                            String sortOrder = sortOrderList != null && sortOrderList.size() > 0 ? sortOrderList.get(0).getSorted().getKey() : null;
                            boolean orderAsc = sortOrderList != null && sortOrderList.size() > 0 ? sortOrderList.get(0).getDirection().equals(SortDirection.ASCENDING) : true;
                            ((ResourcesGridPanel) newTab.getPanel()).showResourcesInternal(currentResourceType, namespaceSelector.getValue(), resourcesFilter, filterText.getValue(), sortOrder, orderAsc, history, historyPointer);
                        });
                        break;
                    }
                }
            }
        });
    }

    private void updateNamespaceSelector(boolean selectDefault) {
        k8s.getNamespacesAsync(true, cluster.getApiProvider()).handle((namespaces, t) -> {
            if (t != null) {
                LOGGER.error("Can't fetch namespaces",t);
                return Collections.emptyList();
            }
            LOGGER.debug("Namespaces: {}",namespaces);
            if (!namespaceSelector.isEmpty() && MCollection.equalsAnyOrder(namespaces, cluster.getCurrentNamespaces()))  {
                if (selectDefault && !MObject.equals(namespaceSelector.getValue(), cluster.getDefaultNamespace())) {
                    core.ui().access(() -> namespaceSelector.setValue(cluster.getDefaultNamespace()));
                }
                return namespaces;
            }
            cluster.setCurrentNamespaces(namespaces);
            core.ui().access(() -> {
                namespaceSelector.setItems(namespaces.stream().sorted().toList());
                core.ui().push();
                if (selectDefault && !MObject.equals(namespaceSelector.getValue(), cluster.getDefaultNamespace())) {
                    Thread.startVirtualThread(() -> {
                        MThread.sleep(600);
                        MLang.await(() -> currentResourceType, 10000);
                        core.ui().access(() -> {
                            namespaceSelector.setValue(cluster.getDefaultNamespace());
                        });
                    });
                }
            });
            return namespaces;
        });
    }

    private void resourceTypeChanged() {
        try {
            var rt = K8s.toResourceType(resourceSelector.getValue());
            if (rt == null || rt.equals(currentResourceType)) return;
            currentResourceType = rt;
            grid = createGrid(rt);
        } catch (NotFoundRuntimeException e) {
            LOGGER.debug("Resource type not found: {}",resourceSelector.getValue());
            grid = createDefaultGrid();
        }
        initGrid();
    }

    private ResourcesGrid createDefaultGrid() {
        var g =  new GenericGrid();
        core.getBeanFactory().autowireBean(g);
        return g;
    }

    private ResourcesGrid createGrid(K8s resourceType) {
        ResourceGridFactory foundFactory = null;
        if (resourceType != null) {
            for (ResourceGridFactory factory : resourceGridFactories)
                if (    factory.canHandleResourceType(resourceType) &&
                        securityService.hasRole(AaaConfiguration.SCOPE_RESOURCE_GRID, factory) &&
                        (foundFactory == null || foundFactory.getPriority(resourceType) > factory.getPriority(resourceType)))
                            foundFactory = factory;
                    }

        ResourcesGrid resourcesGrid = foundFactory.create(resourceType);
        core.getBeanFactory().autowireBean(resourcesGrid);
        LOGGER.debug("Create grid: {}",resourcesGrid.getClass().getSimpleName());
        return resourcesGrid;
    }

    @Override
    public void tabInit(DeskTab deskTab) {
        this.tab = deskTab;
        LOGGER.info("ClusterId: {}",clusterId);
        cluster = clusterService.getCluster(clusterId);
        currentResourceType = cluster.getDefaultResourceType();
        historyMaxSize = viewConfig.getConfig("resourcesGrid").getInt("historyMaxSize", historyMaxSize);

        createUI();

        if (grid != null)
            grid.destroy();
        grid = createGrid(cluster.getDefaultResourceType());
        initGrid();

        final var cc = SecurityContext.create();
        namespaceEventRegistration = getCore().backgroundJobInstance( cluster, NamespaceWatch.class).getEventHandler().register(
                (event) -> {
                    try (var cce = cc.enter()) {
                        if (event.type.equals(K8sUtil.WATCH_EVENT_ADDED) || event.type.equals(K8sUtil.WATCH_EVENT_DELETED))
                            updateNamespaceSelector(false);
                    } catch (Exception e) {
                        LOGGER.error("Error in namespace event",e);
                    }
                }
        );

        historyAdd(cluster.getDefaultResourceType(), cluster.getDefaultNamespace(), null, "", null, true);
    }

    private void initGrid() {

        gridContainer.removeAll();
        if (grid != null) {
            grid.setFilter(filterText.getValue(), resourcesFilter);
            grid.setNamespace(namespaceSelector.getValue());
            if (grid instanceof GenericGrid genericGrid)
                genericGrid.setResourceType(resourceSelector.getValue());
            else
                grid.setResourceType(k8s.findResource(resourceSelector.getValue()));
            namespaceSelector.setEnabled(grid.isNamespaced());
            grid.init(cluster, this);
            var gc = grid.getComponent();
            if (gc instanceof ThemableLayout tl) {
                tl.setMargin(false);
                tl.setPadding(false);
            }
            gridContainer.add(gc);
        }
    }

    @Override
    public void tabSelected() {
        grid.setSelected();
    }

    @Override
    public void tabUnselected() {
        grid.setUnselected();
    }

    @Override
    public void tabDestroyed() {
        if (namespaceEventRegistration != null)
            namespaceEventRegistration.unregister();
        if (grid != null)
            grid.destroy();
        grid = null;
    }

    @Override
    public void tabRefresh(long counter) {
        if (core.ui() != null && grid != null) {
            core.ui().access(() -> {
                LOGGER.trace("Refresh " + grid.getClass().getSimpleName());
                grid.refresh(counter);
            });
        }
    }

    public void showResources(K8s resourceType, String namespace, ResourcesFilter filter, String filterText) {
        historyAdd(resourceType, namespace, filter, filterText, null, true);
        showResourcesInternal(resourceType, namespace, filter, filterText, null, true, history, historyPointer);
    }

    private void showResourcesInternal(K8s resourceType, String namespace, ResourcesFilter filter, String filterText, String sortOrder, boolean sortAscending, LinkedList<HistoryItem> history, int historyPointer) {
        if (filterText != null)
            this.filterText.setValue(filterText);
        setNamespace(namespace);
        if (filter != null)
            setResourcesFilter(filter);
        setResourceType(resourceType);
        if (sortOrder != null) {
            grid.setSortOrder(sortOrder, sortAscending);
        }
        if (history != null) {
            this.history = new LinkedList<>(history);
            this.historyPointer = historyPointer;
            historyUpdate();
        }
    }

    public void setResourceType(K8s resourceType) {
        if (resourceType != null) {
            resourceSelector.setValue(k8s.findResource(resourceType, cluster.getApiProvider()));
            resourceTypeChanged();
        }
    }

    private void setResourcesFilter(ResourcesFilter filter) {
        resourcesFilter = filter;
        resourceFilterButton.setEnabled(true);
        resourceFilterButton.setTooltipText(filter.getDescription());
    }

    public void setNamespace(String namespace) {
        if (namespace == null) return;
        if (namespace.equals(""))
            namespaceSelector.setValue(K8sUtil.NAMESPACE_DEFAULT);
        else if (namespace.equals(K8sUtil.NAMESPACE_ALL))
            namespaceSelector.setValue(K8sUtil.NAMESPACE_ALL_LABEL);
        else
            namespaceSelector.setValue(namespace);
    }

    // for tests
    public List<String> getNamespaces() {
        return Collections.unmodifiableList(cluster.getCurrentNamespaces());
    }

    public void focusFilter() {
        filterText.focus();
    }

    public void focusResources() {
        resourceSelector.focus();
    }

    public void focusNamespaces() {
        namespaceSelector.focus();
    }

    private record HistoryItem(K8s resourceType, String namespace, ResourcesFilter filter, String filterText, String sortOrder, boolean sortAscending) {
    }

}
