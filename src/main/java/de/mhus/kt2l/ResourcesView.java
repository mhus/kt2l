package de.mhus.kt2l;

import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

import static de.mhus.commons.tools.MString.isEmpty;

@Slf4j
public class ResourcesView extends VerticalLayout implements XTabListener {

    @Getter
    private final MainView mainView;
    @Autowired
    @Getter
    private K8sService k8s;
    @Autowired
    @Getter
    Configuration config;
    @Getter
    private String clusterId;
    private ResourcesGrid grid;
    private TextField filterText;
    private ComboBox<String> namespaceSelector;
    private ComboBox<V1APIResource> resourceSelector;
    private CoreV1Api coreApi;
    @Getter
    private UI ui;
    @Getter
    private ClusterConfiguration.Cluster clusterConfig;
    private VerticalLayout gridContainer;
    @Getter
    private String currentResourceType;
    @Getter
    private XTab xTab;

    public ResourcesView(String clusterId, MainView mainView) {
        this.clusterId = clusterId;
        this.mainView = mainView;
    }

    public void createUI() {
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
        K8sUtil.getNamespacesAsync(coreApi).handle((namespaces, t) -> {
            if (t != null) {
                LOGGER.error("Can't fetch namespaces",t);
                return Collections.emptyList();
            }
            LOGGER.debug("Namespaces: {}",namespaces);
            namespaces.addFirst(K8sUtil.NAMESPACE_ALL);
            ui.access(() -> {
                namespaceSelector.setItems(namespaces);
                namespaceSelector.setValue(clusterConfig.defaultNamespace());
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
        resourceSelector.setItemLabelGenerator(new ItemLabelGenerator<V1APIResource>() {
            @Override
            public String apply(V1APIResource item) {
                var shortNames = item.getShortNames();
                var name = item.getSingularName();
                if (isEmpty(name)) name = item.getName();
                return name + (shortNames != null ? " " + shortNames : "");
            }
        });
        resourceSelector.addValueChangeListener(e -> {
            var rt = K8sUtil.toResourceType(e.getValue());
            if (rt == null || rt.equals(currentResourceType)) return;
            grid = createGrid(rt);
            initGrid();
        });

        K8sUtil.getResourceTypesAsync(coreApi).handle((types, t) -> {
            if (t != null) {
                LOGGER.error("Can't fetch resource types",t);
                return Collections.emptyList();
            }
            LOGGER.debug("Resource types: {}",types.stream().map(V1APIResource::getName).toList());
            ui.access(() -> {
                resourceSelector.setItems(types);
                resourceSelector.setValue(
                        K8sUtil.findResource(currentResourceType, types));
            });
            return types;
        });

        // filter text
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> {
            if (grid != null)
                grid.setFilter(e.getValue());
        });

        // toolbar
        var toolbar = new HorizontalLayout(filterText, namespaceSelector,resourceSelector);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private ResourcesGrid createGrid(String resourceType) {
        ResourcesGrid resourcesGrid = resourceType == null ? new GeneralGrid() : switch(resourceType) {
            case K8sUtil.RESOURCE_PODS -> new PodGrid();
            default -> new GeneralGrid();
        };
        mainView.getBeanFactory().autowireBean(resourcesGrid);
        return resourcesGrid;
    }

    @Override
    public void tabInit(XTab xTab) {
        this.xTab = xTab;
        coreApi = Try.of(() -> k8s.getCoreV1Api(clusterId)).onFailure(e -> LOGGER.error("Error ",e) ).get();
        LOGGER.info("ClusterId: {}",clusterId);
        clusterConfig = config.getClusterConfiguration().getClusterOrDefault(clusterId);
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
            grid.setFilter(filterText.getValue());
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
    public void tabDeselected() {

    }

    @Override
    public void tabDestroyed() {
        if (grid != null)
            grid.destroy();
        grid = null;
    }

    @Override
    public void tabRefresh() {
        if (ui != null && grid != null) {
            ui.access(() -> {
                LOGGER.info("Refresh " + grid.getClass().getSimpleName());
                grid.refresh();
            });
        }
    }

    @Override
    public void tabShortcut(ShortcutEvent event) {
        grid.handleShortcut(event);
    }

}
