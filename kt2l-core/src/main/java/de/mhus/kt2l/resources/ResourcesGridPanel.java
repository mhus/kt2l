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

import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.mhus.commons.errors.NotFoundRuntimeException;
import de.mhus.commons.lang.IRegistration;
import de.mhus.commons.tools.MCollection;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tools.MSystem;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterConfiguration;
import de.mhus.kt2l.config.AaaConfiguration;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.SecurityContext;
import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.core.XTab;
import de.mhus.kt2l.core.XTabListener;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.generic.GenericGrid;
import de.mhus.kt2l.resources.generic.GenericGridFactory;
import de.mhus.kt2l.resources.namespace.NamespaceWatch;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import static de.mhus.commons.tools.MString.isEmpty;

@Slf4j
public class ResourcesGridPanel extends VerticalLayout implements XTabListener {

    private static final ResourceGridFactory GENERIC_GRID_FACTORY = new GenericGridFactory();
    @Getter
    private final Core core;

    @Autowired
    @Getter
    private K8sService k8s;

    @Autowired
    @Getter
    private ClusterConfiguration clusterConfiguration;

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
    private CoreV1Api coreApi;
    @Getter
    private UI ui;
    @Getter
    private Cluster cluster;
    private VerticalLayout gridContainer;
    @Getter
    private K8s.RESOURCE currentResourceType;
    @Getter
    private XTab xTab;
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
        resourceSelector = new ComboBox<>();

        addClassName("list-view");
        setSizeFull();

        gridContainer = new VerticalLayout();
        gridContainer.setSizeFull();

        add(getToolbar(), gridContainer);
        this.ui = UI.getCurrent();

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

        k8s.getResourceTypesAsync(coreApi).handle((types, t) -> {
            if (t != null) {
                LOGGER.error("Can't fetch resource types",t);
                return Collections.emptyList();
            }
            cluster.setResourceTypes(types);
            LOGGER.debug("Resource types: {}",types.stream().map(V1APIResource::getName).toList());
            ui.access(() -> {
                resourceSelector.setItems(cluster.getResourceTypes());
                Thread.startVirtualThread(() -> {
                    MThread.sleep(200);
                    ui.access(() -> {
                        resourceSelector.setValue(
                                k8s.findResource(currentResourceType, coreApi, principal));
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
        return toolbar;
    }

    private void updateNamespaceSelector(boolean selectDefault) {
        k8s.getNamespacesAsync(true, coreApi).handle((namespaces, t) -> {
            if (t != null) {
                LOGGER.error("Can't fetch namespaces",t);
                return Collections.emptyList();
            }
            LOGGER.debug("Namespaces: {}",namespaces);
            if (MCollection.equalsAnyOrder(namespaces, cluster.getCurrentNamespaces()))  {
                if (selectDefault && !MSystem.equals(namespaceSelector.getValue(), cluster.getDefaultNamespace())) {
                    ui.access(() -> namespaceSelector.setValue(cluster.getDefaultNamespace()));
                }
                return namespaces;
            }
            cluster.setCurrentNamespaces(namespaces);
            ui.access(() -> {
                namespaceSelector.setItems(namespaces);
                ui.push();
                if (selectDefault && !MSystem.equals(namespaceSelector.getValue(), cluster.getDefaultNamespace())) {
                    Thread.startVirtualThread(() -> {
                        MThread.sleep(200);
                        ui.access(() -> {
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

    private ResourcesGrid createGrid(K8s.RESOURCE resourceType) {
        ResourceGridFactory foundFactory = GENERIC_GRID_FACTORY;
        if (resourceType != null) {
            for (ResourceGridFactory factory : resourceGridFactories)
                if (    factory.canHandleResourceType(resourceType) &&
                        securityService.hasRole(AaaConfiguration.SCOPE_RESOURCE_GRID, factory) &&
                        foundFactory.getPriority(resourceType) > factory.getPriority(resourceType))
                            foundFactory = factory;
                    }

        ResourcesGrid resourcesGrid = foundFactory.create(resourceType);
        core.getBeanFactory().autowireBean(resourcesGrid);
        LOGGER.debug("Create grid: {}",resourcesGrid.getClass().getSimpleName());
        return resourcesGrid;
    }

    @Override
    public void tabInit(XTab xTab) {
        this.xTab = xTab;
        coreApi = Try.of(() -> k8s.getCoreV1Api(clusterId)).onFailure(e -> LOGGER.error("Error ",e) ).get();
        LOGGER.info("ClusterId: {}",clusterId);
        cluster = clusterConfiguration.getClusterOrDefault(clusterId);
        currentResourceType = cluster.getDefaultResourceType();

        createUI();

        if (grid != null)
            grid.destroy();
        grid = createGrid(cluster.getDefaultResourceType());
        initGrid();

        final var cc = SecurityContext.create();
        namespaceEventRegistration = NamespaceWatch.instance(getCore(), cluster).getEventHandler().register(
                (event) -> {
                    try (var cce = cc.enter()) {
                        if (event.type.equals(K8s.WATCH_EVENT_ADDED) || event.type.equals(K8s.WATCH_EVENT_DELETED))
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
            grid.init(coreApi, cluster, this);
            gridContainer.add(grid.getComponent());
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
        if (ui != null && grid != null) {
            ui.access(() -> {
                LOGGER.trace("Refresh " + grid.getClass().getSimpleName());
                grid.refresh(counter);
            });
        }
    }

    @Override
    public void tabShortcut(ShortcutEvent event) {
        grid.handleShortcut(event);
    }

    public void showResources(K8s.RESOURCE resourceType, ResourcesFilter filter) {
        if (filter != null)
            setResourcesFilter(filter);
        if (resourceType != null) {
            resourceSelector.setValue(k8s.findResource(resourceType, coreApi));
            resourceTypeChanged();
        }
    }


    private void setResourcesFilter(ResourcesFilter filter) {
        resourcesFilter = filter;
        resourceFilterButton.setEnabled(true);
        resourceFilterButton.setTooltipText(filter.getDescription());
    }

    public void setNamespace(String namespace) {
        namespaceSelector.setValue(namespace);
    }

    // for tests
    public List<String> getNamespaces() {
        return Collections.unmodifiableList(cluster.getCurrentNamespaces());
    }
}
