package de.mhus.kt2l;

import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static de.mhus.commons.tools.MString.isEmpty;

@Slf4j
public class ResourcesView extends VerticalLayout implements XTabListener {

    @Autowired
    private K8sService k8s;

    @Autowired
    ScheduledExecutorService scheduler;

    @Autowired
    Configuration config;

    private String clusterId;
    private ResourcesGrid grid;
    private TextField filterText;
    private ComboBox<String> namespaceSelector;
    private ComboBox<V1APIResource> resourceSelector;
    private CoreV1Api coreApi;
    private UI ui;
    private ScheduledFuture<?> closeScheduler;
    private ClusterConfiguration.Cluster clusterConfig;
    private VerticalLayout gridContainer;
    private String currentResourceType;

    public ResourcesView(String clusterId) {
        this.clusterId = clusterId;
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

//    @Scheduled(fixedRate = 10000)
    public void refresh() {
        if (ui != null && grid != null) {
            ui.access(() -> {
                LOGGER.info("Refresh " + grid.getClass().getSimpleName());
                grid.refresh();
            });
        }
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
            LOGGER.debug("Resource types: {}",types);
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
        if (resourceType != null) {
            if (resourceType.equals(K8sUtil.RESOURCE_PODS))
                return new PodGrid();
        }
        return new GeneralGrid();
    }

    @Override
    public void tabInit(XTab xTab) {
        coreApi = Try.of(() -> k8s.getCoreV1Api(clusterId)).onFailure(e -> LOGGER.error("Error ",e) ).get();
        LOGGER.info("ClusterId: {}",clusterId);
        clusterConfig = config.getClusterConfiguration().getClusterOrDefault(clusterId);
        currentResourceType = clusterConfig.defaultResourceType();

        createUI();

        grid = createGrid(clusterConfig.defaultResourceType());
        initGrid();

        closeScheduler = scheduler.scheduleAtFixedRate(this::refresh, 10, 10, java.util.concurrent.TimeUnit.SECONDS);

    }

    private void initGrid() {

        gridContainer.removeAll();
        if (grid != null) {
            grid.setFilter(filterText.getValue());
            grid.setNamespace(namespaceSelector.getValue());
            grid.setResourceType(K8sUtil.toResourceType(resourceSelector.getValue()));
            grid.init(coreApi, clusterConfig);
            gridContainer.add(grid.getComponent());
        }
    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabClosed() {

    }

    @Override
    public void tabDestroyed() {
        LOGGER.info("Leave");
        closeScheduler.cancel(false);
    }

}
