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
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterConfiguration;
import de.mhus.kt2l.config.AaaConfiguration;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.generic.GenericGridFactory;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.core.XTab;
import de.mhus.kt2l.core.XTabListener;
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

    private ResourcesGrid grid;
    private TextField filterText;
    private ComboBox<String> namespaceSelector;
    private ComboBox<V1APIResource> resourceSelector;
    private CoreV1Api coreApi;
    @Getter
    private UI ui;
    @Getter
    private Cluster clusterConfig;
    private VerticalLayout gridContainer;
    @Getter
    private String currentResourceType;
    @Getter
    private XTab xTab;
    private ResourcesFilter resourcesFilter;
    private Button resourceFilterButton;

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

        k8s.getNamespacesAsync(true, coreApi).handle((namespaces, t) -> {
            if (t != null) {
                LOGGER.error("Can't fetch namespaces",t);
                return Collections.emptyList();
            }
            LOGGER.debug("Namespaces: {}",namespaces);
            ui.access(() -> {
                namespaceSelector.setItems(namespaces);
                Thread.startVirtualThread(() -> {
                    MThread.sleep(200);
                    ui.access(() -> {
                        namespaceSelector.setValue(clusterConfig.defaultNamespace());
                    });
                });
            });
            return namespaces;
        });
        namespaceSelector.addValueChangeListener(e -> {
            if (grid != null)
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
            final var typesFinal = types;
            LOGGER.debug("Resource types: {}",types.stream().map(V1APIResource::getName).toList());
            ui.access(() -> {
                resourceSelector.setItems(typesFinal);
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

    private void resourceTypeChanged() {
        var rt = K8sUtil.toResourceType(resourceSelector.getValue());
        if (rt == null || rt.equals(currentResourceType)) return;
        currentResourceType = rt;
        grid = createGrid(rt);
        initGrid();
    }

    private ResourcesGrid createGrid(String resourceType) {
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
        return resourcesGrid;
    }

    @Override
    public void tabInit(XTab xTab) {
        this.xTab = xTab;
        coreApi = Try.of(() -> k8s.getCoreV1Api(clusterId)).onFailure(e -> LOGGER.error("Error ",e) ).get();
        LOGGER.info("ClusterId: {}",clusterId);
        clusterConfig = clusterConfiguration.getClusterOrDefault(clusterId);
        currentResourceType = clusterConfig.defaultResourceType();

        createUI();

        if (grid != null)
            grid.destroy();
        grid = createGrid(clusterConfig.defaultResourceType());
        initGrid();

    }

    private void initGrid() {

        gridContainer.removeAll();
        if (grid != null) {
            grid.setFilter(filterText.getValue(), resourcesFilter);
            grid.setNamespace(namespaceSelector.getValue());
            grid.setResourceType(K8sUtil.toResourceType(resourceSelector.getValue()));
            grid.init(coreApi, clusterConfig, this);
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

    public void showResources(String resourceType, ResourcesFilter filter) {
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
}
