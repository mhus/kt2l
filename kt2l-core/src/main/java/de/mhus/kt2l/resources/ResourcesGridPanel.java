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
import de.mhus.kt2l.cluster.ClusterConfiguration;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.generic.GenericGridFactory;
import de.mhus.kt2l.core.MainView;
import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.core.XTab;
import de.mhus.kt2l.core.XTabListener;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

import static de.mhus.commons.tools.MString.isEmpty;

@Slf4j
public class ResourcesGridPanel extends VerticalLayout implements XTabListener {

    private static final ResourceGridFactory GENERIC_GRID_FACTORY = new GenericGridFactory();
    @Getter
    private final MainView mainView;

    @Autowired
    @Getter
    private K8sService k8s;

    @Autowired
    @Getter
    private Configuration config;

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
    private ClusterConfiguration.Cluster clusterConfig;
    private VerticalLayout gridContainer;
    @Getter
    private String currentResourceType;
    @Getter
    private XTab xTab;
    private List<V1APIResource> resourceList;
    private ResourcesFilter resourcesFilter;
    private Button resourceFilterButton;

    public ResourcesGridPanel(String clusterId, MainView mainView) {
        this.clusterId = clusterId;
        this.mainView = mainView;
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
        K8sUtil.getNamespacesAsync(coreApi).handle((namespaces, t) -> {
            if (t != null) {
                LOGGER.error("Can't fetch namespaces",t);
                return Collections.emptyList();
            }
            LOGGER.debug("Namespaces: {}",namespaces);
            namespaces.addFirst(K8sUtil.NAMESPACE_ALL);
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
            resourceTypeChanged();
        });

        K8sUtil.getResourceTypesAsync(coreApi).handle((types, t) -> {
            resourceList = Collections.synchronizedList(types);
            if (t != null) {
                LOGGER.error("Can't fetch resource types",t);
                return Collections.emptyList();
            }
            LOGGER.debug("Resource types: {}",types.stream().map(V1APIResource::getName).toList());
            ui.access(() -> {
                resourceSelector.setItems(types);
                Thread.startVirtualThread(() -> {
                    MThread.sleep(200);
                    ui.access(() -> {
                        resourceSelector.setValue(
                                K8sUtil.findResource(currentResourceType, resourceList));
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
                        securityService.hasRole(factory) &&
                        foundFactory.getPriority(resourceType) > factory.getPriority(resourceType))
                            foundFactory = factory;
                    }

        ResourcesGrid resourcesGrid = foundFactory.create(resourceType);
        mainView.getBeanFactory().autowireBean(resourcesGrid);
        return resourcesGrid;
    }

    /*
    TODO handle:
java.io.IOException: Cannot run program "gke-gcloud-auth-plugin": error=2, No such file or directory
	at java.base/java.lang.ProcessBuilder.start(Unknown Source)
	at java.base/java.lang.ProcessBuilder.start(Unknown Source)
	at io.kubernetes.client.util.KubeConfig.runExec(KubeConfig.java:345)
	at io.kubernetes.client.util.KubeConfig.credentialsViaExecCredential(KubeConfig.java:281)
	at io.kubernetes.client.util.KubeConfig.getCredentials(KubeConfig.java:237)
	at io.kubernetes.client.util.credentials.KubeconfigAuthentication.<init>(KubeconfigAuthentication.java:59)
	at io.kubernetes.client.util.ClientBuilder.kubeconfig(ClientBuilder.java:299)
	at io.kubernetes.client.util.Config.fromConfig(Config.java:98)
	at de.mhus.kt2l.k8s.K8sService.getKubeClient(K8sService.java:98)
	at de.mhus.kt2l.k8s.K8sService.getCoreV1Api(K8sService.java:102)
	at de.mhus.kt2l.resources.ResourcesGridPanel.lambda$tabInit$7518f186$1(ResourcesGridPanel.java:198)
	at io.vavr.control.Try.of(Try.java:83)
	at de.mhus.kt2l.resources.ResourcesGridPanel.tabInit(ResourcesGridPanel.java:198)
	at de.mhus.kt2l.ui.XTabBar.lambda$addTab$0(XTabBar.java:45)
	at io.vavr.control.Try.run(Try.java:154)

io.kubernetes.client.openapi.ApiException: java.net.ConnectException: Failed to connect to /127.0.0.1:6443
	at io.kubernetes.client.openapi.ApiClient.execute(ApiClient.java:888)
	at io.kubernetes.client.openapi.apis.CoreV1Api.listPodForAllNamespacesWithHttpInfo(CoreV1Api.java:37296)
	at io.kubernetes.client.openapi.apis.CoreV1Api.listPodForAllNamespaces(CoreV1Api.java:37189)
	at de.mhus.kt2l.pods.PodGrid$PodProvider.lambda$new$56cc5271$1(PodGrid.java:294)
	at io.vavr.control.Try.of(Try.java:83)
	at de.mhus.kt2l.pods.PodGrid$PodProvider.lambda$new$512e4660$1(PodGrid.java:294)
	at com.vaadin.flow.data.provider.CallbackDataProvider.sizeInBackEnd(CallbackDataProvider.java:142)
	at com.vaadin.flow.data.provider.AbstractBackEndDataProvider.size(AbstractBackEndDataProvider.java:66)
	at com.vaadin.flow.data.provider.DataCommunicator.getDataProviderSize(DataCommunicator.java:940)
	at com.vaadin.flow.data.provider.DataCommunicator.flush(DataCommunicator.java:1193)
	at com.vaadin.flow.data.provider.DataCommunicator.lambda$requestFlush$7258256f$1(DataCommunicator.java:1138)
	at com.vaadin.flow.internal.StateTree.lambda$runExecutionsBeforeClientResponse$2(StateTree.java:397)
	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(Unknown Source)
	at java.base/java.util.stream.ReferencePipeline$2$1.accept(Unknown Source)
	at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(Unknown Source)

     */
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
            resourceSelector.setValue(K8sUtil.findResource(resourceType, resourceList));
            resourceTypeChanged();
        }
    }


    private void setResourcesFilter(ResourcesFilter filter) {
        resourcesFilter = filter;
        resourceFilterButton.setEnabled(true);
        resourceFilterButton.setTooltipText(filter.getDescription());
    }
}
