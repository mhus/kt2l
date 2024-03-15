package de.mhus.kt2l;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import de.mhus.commons.lang.IRegistration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Watch;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.mhus.commons.tools.MCollection.cropArray;

@Slf4j
public class PodGrid extends VerticalLayout implements ResourcesGrid {

    private List<Pod> podList = null;
    private List<Pod> filteredList = null;
    private List<Container> containerList = null;
    private String filterText = "";
    private String namespace;
    private CoreV1Api coreApi;
    private ClusterConfiguration.Cluster clusterConfig;
    private Grid<Pod> podGrid;
    private Grid<Container> containerGrid;
    private MenuBar menuBar;
    private List<MenuAction> actions = new ArrayList<>(10);

    @Autowired
    private ActionService actionService;
    private ResourcesGridPanel view;
    private Pod containerSelectedPod;
    private Optional<Pod> selectedPod;
    private IRegistration podEventRegistration;

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void refresh(long counter) {
        if (counter % 10 != 0) return;
//        podList = null;
        filterList();
        podGrid.getDataProvider().refreshAll();
        UI.getCurrent().push();
    }

    @Override
    public void init(CoreV1Api coreApi, ClusterConfiguration.Cluster clusterConfig, ResourcesGridPanel view) {
        this.coreApi = coreApi;
        this.view = view;
        this.clusterConfig = clusterConfig;

        createPodGrid();
        createContainerGrid();
        createMenuBar();

        add(menuBar, podGrid, containerGrid);
        setSizeFull();

        actions.forEach(a -> a.updateWithPod(Collections.emptySet()));

        podEventRegistration = view.getMainView().getBackgroundJob(clusterConfig.name(), ClusterPodWatch.class, () -> new ClusterPodWatch()).getPodEventHandler().registerWeak(this::podEvent);

    }

    private void podEvent(Watch.Response<V1Pod> event) {
        if (podList == null) return;
        if (namespace != null && !namespace.equals(K8sUtil.NAMESPACE_ALL) && !namespace.equals(event.object.getMetadata().getNamespace())) return;

        if (event.type.equals(K8sUtil.WATCH_EVENT_ADDED) || event.type.equals(K8sUtil.WATCH_EVENT_MODIFIED)) {

            final var foundPod = podList.stream().filter(pod -> pod.getName().equals(event.object.getMetadata().getName())).findFirst().orElseGet(
                () -> {
                    final var pod = new Pod(
                            event.object.getMetadata().getName(),
                            event.object.getMetadata().getNamespace(),
                            event.object.getStatus().getPhase(),
                            event.object.getMetadata().getCreationTimestamp().toEpochSecond(),
                            event.object
                    );
                    podList.add(pod);
                    return pod;
                }
            );

            foundPod.setStatus(event.object.getStatus().getPhase());
            foundPod.setPod(event.object);
            filterList();
            podGrid.getDataProvider().refreshItem(foundPod);
        }

        if (event.type.equals(K8sUtil.WATCH_EVENT_DELETED)) {
            podList.forEach(pod -> {
                if (pod.getName().equals(event.object.getMetadata().getName())) {
                    podList.remove(pod);
                    filterList();
                    podGrid.getDataProvider().refreshAll();
                }
            });
        }

    }

    private void createMenuBar() {
        menuBar = new MenuBar();

        actionService.findActionsForResource(K8sUtil.RESOURCE_PODS).forEach(action -> {

            final MenuAction menuAction = new MenuAction();

            MenuItem item = menuBar.addItem(action.getTitle(), new ComponentEventListener<ClickEvent<MenuItem>>() {
                @Override
                public void onComponentEvent(ClickEvent<MenuItem> event) {
                    menuAction.execute();
                }
            });
            item.setEnabled(false);
            item.getElement().setAttribute("title", action.getDescription() + (action.getShortcutKey() == null ? "" : " (" + action.getShortcutKey() + ")" ));

            menuAction.setAction(action);
            menuAction.setMenuItem(item);

            actions.add(menuAction);

            if (action.getShortcutKey() != null) {
                final var k1 = action.getShortcutKey().split("\\+");
                final var key = Key.of(k1[k1.length-1], cropArray(k1, 0, k1.length-1));
                if (key != null) {
                    UI.getCurrent().addShortcutListener(() -> {
                        menuAction.execute();
                    },key).listenOn(podGrid, containerGrid);
                }
            }

        });

    }
    private void createContainerGrid() {
        containerGrid = new Grid<>(Container.class, false);
        addClassNames("contact-grid");
        containerGrid.setWidthFull();
        containerGrid.setHeight("200px");
        containerGrid.addColumn(cont -> cont.name()).setHeader("Name").setSortProperty("name");
        containerGrid.addColumn(cont -> cont.status()).setHeader("Status").setSortProperty("status");
        containerGrid.addColumn(cont -> cont.age()).setHeader("Age").setSortProperty("age");
        containerGrid.getColumns().forEach(col -> col.setAutoWidth(true));
        containerGrid.setDataProvider(new ContainerProvider());
        containerGrid.setVisible(false);
        containerGrid.addSelectionListener(event -> {
            if (containerGrid.isVisible())
                actions.forEach(a -> a.updateWithContainer(event.getAllSelectedItems()));
        });
    }

    private void createPodGrid() {

        podGrid = new Grid<>(Pod.class, false);
        addClassNames("contact-grid");
        podGrid.setSizeFull();
        podGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        podGrid.addColumn(pod -> pod.getName()).setHeader("Name").setSortProperty("name");
        podGrid.addColumn(pod -> pod.getStatus()).setHeader("Status").setSortProperty("status");
        podGrid.addColumn(pod -> pod.getAge()).setHeader("Age").setSortProperty("age");
        podGrid.getColumns().forEach(col -> col.setAutoWidth(true));
        podGrid.setDataProvider(new PodProvider());

        podGrid.addCellFocusListener(event -> {
            selectedPod = event.getItem();
            if (selectedPod.isPresent())
                setContainerPod(selectedPod.get());
        });

        podGrid.addSelectionListener(event -> {
            if (containerGrid.isVisible())
                containerGrid.deselectAll();
            actions.forEach(a -> a.updateWithPod(event.getAllSelectedItems()));
        });
        podGrid.addItemClickListener(event -> {
            if (event.getClickCount() == 2) {
                flipContainerVisibility(event.getItem(), false, false);
            } else
            if (event.getClickCount() == 1) {
                setContainerPod(event.getItem());
                if (event.isAltKey()) {
                    if (podGrid.getSelectionModel().isSelected(event.getItem()))
                        podGrid.getSelectionModel().deselect(event.getItem());
                    else
                        podGrid.getSelectionModel().select(event.getItem());
                } else
                if (event.isShiftKey()) {
                    var first = podGrid.getSelectionModel().getFirstSelectedItem();
                    if (first == null) {
                        podGrid.getSelectionModel().select(event.getItem());
                    } else {
                        var start = filteredList.indexOf(first.get());
                        var end = filteredList.indexOf(event.getItem());
                        if (start > end) {
                            var tmp = start;
                            start = end;
                            end = tmp;
                        }
                        podGrid.getSelectionModel().deselectAll();
                        for (int i = start; i <= end; i++) {
                            podGrid.getSelectionModel().select(filteredList.get(i));
                        }
                    }
                } else {
                    if (podGrid.getSelectionModel().isSelected(event.getItem()))
                        podGrid.getSelectionModel().deselectAll();
                    else {
                        podGrid.getSelectionModel().deselectAll();
                        podGrid.getSelectionModel().select(event.getItem());
                    }
                }
            }
        });

        UI.getCurrent().addShortcutListener(this::handleShortcut, Key.SPACE).listenOn(podGrid);
        UI.getCurrent().addShortcutListener(this::handleShortcut, Key.ENTER).listenOn(podGrid);

    }

    private void setContainerPod(Pod item) {
        if (containerGrid.isVisible()) {
            containerSelectedPod = item;
            containerList = null;
            containerGrid.getDataProvider().refreshAll();
        }
    }

    @Override
    public void setFilter(String value) {
        filterText = value;
        if (podList != null)
            podGrid.getDataProvider().refreshAll();
    }

    @Override
    public void setNamespace(String value) {
        namespace = value;
        if (podList != null) {
            podList = null;
            podGrid.getDataProvider().refreshAll();
        }
    }

    @Override
    public void setResourceType(String resourceType) {

    }

    @Override
    public void handleShortcut(ShortcutEvent event) {
        if (event.getKey().matches(" ") && event.getKeyModifiers().size() == 0) {
            if (selectedPod != null && selectedPod.isPresent()) {
                if (podGrid.getSelectionModel().isSelected(selectedPod.get()))
                    podGrid.getSelectionModel().deselect(selectedPod.get());
                else
                    podGrid.getSelectionModel().select(selectedPod.get());
            }
            return;
        }
        if (event.getKey().matches(Key.ENTER.toString()) && event.getKeyModifiers().size() == 0) {
            if (selectedPod != null && selectedPod.isPresent()) {
                flipContainerVisibility(selectedPod.get(), true, true);
            }
            return;
        }

    }

    private void flipContainerVisibility(Pod pod, boolean alwaysVisible, boolean focus) {
        containerList = null;
        containerSelectedPod = null;
        containerGrid.setVisible(alwaysVisible || !containerGrid.isVisible());
        if (containerGrid.isVisible()) {
            containerSelectedPod = pod;
            containerGrid.getDataProvider().refreshAll();
            if (focus)
                containerGrid.getElement().getNode()
                        .runWhenAttached(ui -> ui.getPage().executeJs(
                                "setTimeout(function(){let firstTd = $0.shadowRoot.querySelector('tr:first-child > td:first-child'); firstTd.click(); firstTd.focus(); },0)", containerGrid.getElement()));
        }

    }

    @Override
    public void setSelected() {

        podGrid.getElement().getNode()
                .runWhenAttached(ui -> ui.getPage().executeJs(
                        "setTimeout(function(){let firstTd = $0.shadowRoot.querySelector('tr:first-child > td:first-child'); firstTd.click(); firstTd.focus(); },0)", podGrid.getElement()));

    }

    @Override
    public void setUnselected() {
    }

    @Override
    public void destroy() {
        podEventRegistration.unregister();
    }

    private class ContainerProvider extends CallbackDataProvider<Container, Void> {
        public ContainerProvider() {
            super(query -> {
                        LOGGER.debug("Cont: Do the query {}",query);
                        for(QuerySortOrder queryOrder :
                                query.getSortOrders()) {
                            Collections.sort(containerList, (a, b) -> switch (queryOrder.getSorted()) {
                                case "name" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> a.name().compareTo(b.name());
                                    case DESCENDING -> b.name().compareTo(a.name());
                                };
                                case "status" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> a.status().compareTo(b.status());
                                    case DESCENDING -> b.status().compareTo(a.status());
                                };
                                case "age" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> Long.compare(a.created(), b.created());
                                    case DESCENDING -> Long.compare(b.created(), a.created());
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
                                            containerList.add(new Container(
                                                    cs.getName(),
                                                    selectedPod.getNamespace(),
                                                    cs.getState().getWaiting() != null ? "Waiting" : "Running",
                                                    getAge(containerSelectedPod.getPod().getMetadata().getCreationTimestamp()), //TODO use container start time
                                                    selectedPod.getPod().getMetadata().getCreationTimestamp().toEpochSecond(),
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
                        LOGGER.debug("Do the query {}",query);
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
                                default -> 0;
                            });

                        }
                        return filteredList.stream().skip(query.getOffset()).limit(query.getLimit());
                    }, query -> {
                        LOGGER.debug("Do the size query {}",query);
                        if (podList == null) {
                            podList = new ArrayList<>();
                            final var namespaceName = namespace ==  null || namespace.equals(K8sUtil.NAMESPACE_ALL) ? null : (String) namespace;
                            Try.of(() -> namespaceName == null ? coreApi.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null ) :  coreApi.listNamespacedPod(namespaceName, null, null, null, null, null, null, null, null, null, null))
                                    .onFailure(e -> LOGGER.error("Can't fetch pods from cluster",e))
                                    .onSuccess(podList -> {
                                        podList.getItems().forEach(pod -> {
                                            PodGrid.this.podList.add(new Pod(
                                                    pod.getMetadata().getName(),
                                                    pod.getMetadata().getNamespace(),
                                                    pod.getStatus().getPhase(),
                                                    pod.getMetadata().getCreationTimestamp().toEpochSecond(),
                                                    pod
                                            ));
                                        });
                                    });
                        }
                        filterList();
                        return filteredList.size();
                    }
            );
        }

    }

    private String getAge(OffsetDateTime creationTimestamp) {
        final var age = System.currentTimeMillis()/1000 - creationTimestamp.toEpochSecond();
        if (age < 60) return age + "s";
        if (age < 3600) return age/60 + "m";
        if (age < 86400) return age/3600 + "h";
        return age/86400 + "d";
    }

    private void filterList() {
        if (podList == null) {
            filteredList = Collections.emptyList();
        } else {
            final var filter = filterText;
            if (filter.isBlank()) {
                filteredList = podList;
            } if (filter.startsWith("/")) {
                var f = filter.substring(1);
                filteredList = podList.stream().filter(pod -> pod.getName().matches(f)).collect(Collectors.toList());
            } else {
                filteredList = podList.stream().filter(pod -> pod.getName().contains(filter)).collect(Collectors.toList());
            }
        }
    }

    @Data
    @AllArgsConstructor
    public class Pod implements IResourceProvider {
            String name;
            String namespace;
            String status;
            long created;
            V1Pod pod;

            public String getAge() {
                return PodGrid.this.getAge(pod.getMetadata().getCreationTimestamp());
            }

        @Override
        public Object getResource() {
            return pod;
        }
    }

    public record Container(
            String name,
            String namespace,
            String status,
            String age,
            long created,
            V1Pod pod
    ) implements IResourceProvider {

        @Override
        public Object getResource() {
            return pod;
        }
    }

    @Getter
    @Setter
    private class MenuAction {
        ResourceAction action;
        MenuItem menuItem;

        public void updateWithContainer(Set<Container> selected) {
            menuItem.setEnabled(action.canHandleResource(K8sUtil.RESOURCE_CONTAINER,
                    selected == null ? Collections.emptySet() : selected));
        }
        public void updateWithPod(Set<Pod> selected) {
            menuItem.setEnabled(action.canHandleResource(K8sUtil.RESOURCE_PODS,
                    selected == null ? Collections.emptySet() : selected));
        }
        public void execute() {
            ExecutionContext context = null;
            if (containerGrid.isVisible() && containerGrid.getSelectedItems() != null && containerGrid.getSelectedItems().size() == 1) {

                if (!action.canHandleResource(K8sUtil.RESOURCE_CONTAINER, containerGrid.getSelectedItems())) {
                    Notification notification = Notification
                            .show("Can't execute");
                    notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
                    return;
                }

                context = ExecutionContext.builder()
                        .resourceType(K8sUtil.RESOURCE_CONTAINER)
                        .selected(containerGrid.getSelectedItems())
                        .namespace(namespace)
                        .api(coreApi)
                        .clusterConfiguration(clusterConfig)
                        .ui(UI.getCurrent())
                        .grid(PodGrid.this)
                        .mainView(view.getMainView())
                        .selectedTab(view.getXTab())
                        .build();

            } else {
                if (!action.canHandleResource(K8sUtil.RESOURCE_PODS, podGrid.getSelectedItems())) {
                    Notification notification = Notification
                            .show("Can't execute");
                    notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
                    return;
                }
                context = ExecutionContext.builder()
                        .resourceType(K8sUtil.RESOURCE_PODS)
                        .selected(podGrid.getSelectedItems())
                        .namespace(namespace)
                        .api(coreApi)
                        .clusterConfiguration(clusterConfig)
                        .ui(UI.getCurrent())
                        .grid(PodGrid.this)
                        .mainView(view.getMainView())
                        .selectedTab(view.getXTab())
                        .build();
            }

            try {
                action.execute(context);
            } catch (Exception e) {
                LOGGER.error("Error executing action", e);
                Notification notification = Notification
                        .show("Error\n" + e.getMessage());
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

        }
    }
}


