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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.ThemableLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.mhus.commons.errors.NotFoundRuntimeException;
import de.mhus.commons.lang.IRegistration;
import de.mhus.commons.tools.MCollection;
import de.mhus.commons.tools.MObject;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterService;
import de.mhus.kt2l.config.AaaConfiguration;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
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

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import static de.mhus.commons.tools.MString.isEmpty;

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
            }
        });
        resourceFilterButton.setEnabled(false);

        filterText = new TextField();
        namespaceSelector = new ComboBox<>();
        namespaceSelector.addFocusListener(e -> namespaceSelector.getElement().executeJs("this.querySelector(\"input\").select()") );
        resourceSelector = new ComboBox<>();
        resourceSelector.setRenderer(new ComponentRenderer<Component, V1APIResource>(item -> {
            Div div = new Div(item.getName() + (item.getShortNames() == null ? "" : " " + item.getShortNames()));
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
            if (grid != null && !MString.equals(e.getValue(), grid.getNamespace()))
                grid.setNamespace(e.getValue());
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
        });

        final Principal principal = securityService.getPrincipal();

        k8s.getResourceTypesAsync(cluster.getApiProvider()).handle((types, t) -> {
            if (t != null) {
                LOGGER.error("Can't fetch resource types",t);
                return Collections.emptyList();
            }
            cluster.setResourceTypes(types);
            LOGGER.debug("Resource types: {}",types.stream().map(V1APIResource::getName).toList());
            core.ui().access(() -> {
                resourceSelector.setItems(cluster.getResourceTypes().stream().filter(r -> !r.getName().equals("GENERIC") && !r.getName().equals("CUSTOM") && r.getName().indexOf('/') < 0).toList());
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
            if (grid != null)
                grid.setFilter(e.getValue(), resourcesFilter);
        });

        // toolbar
        var toolbar = new HorizontalLayout(resourceFilterButton, filterText, namespaceSelector,resourceSelector);
        toolbar.addClassName("toolbar");
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setMargin(false);
        return toolbar;
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
                namespaceSelector.setItems(namespaces);
                core.ui().push();
                if (selectDefault && !MObject.equals(namespaceSelector.getValue(), cluster.getDefaultNamespace())) {
                    Thread.startVirtualThread(() -> {
                        MThread.sleep(200);
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
        return new GenericGrid();
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

    public void showResources(K8s resourceType, String namespace, ResourcesFilter filter) {
        setNamespace(namespace);
        if (filter != null)
            setResourcesFilter(filter);
        setResourceType(resourceType);
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
}
