package de.mhus.kt2l;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import de.mhus.commons.errors.UsageException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class PodGrid extends VerticalLayout implements ResourcesGrid {

    private List<Pod> podList = null;
    private List<Pod> filteredList = null;
    private String filterText = "";
    private String namespace;
    private CoreV1Api coreApi;
    private ClusterConfiguration.Cluster clusterConfig;
    private Grid<Pod> grid;
    private MenuBar menuBar;
    private List<MenuAction> actions = new ArrayList<>(10);

    @Autowired
    private ActionService actionService;
    private ResourcesView view;

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void refresh() {
        podList = null;
        grid.getDataProvider().refreshAll();
        UI.getCurrent().push();
    }

    @Override
    public void init(CoreV1Api coreApi, ClusterConfiguration.Cluster clusterConfig, ResourcesView view) {
        this.coreApi = coreApi;
        this.view = view;
        this.clusterConfig = clusterConfig;

        createMenuBar();
        createGrid();

        add(menuBar,grid);
        setSizeFull();

        actions.forEach(a -> a.update(Collections.emptySet()));

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

            menuAction.setAction(action);
            menuAction.setMenuItem(item);

            actions.add(menuAction);

        });

    }

    private void createGrid() {
        grid = new Grid<>(Pod.class, false);
        addClassNames("contact-grid");
        grid.setSizeFull();
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addColumn(pod -> pod.name()).setHeader("Name").setSortProperty("name");
        grid.addColumn(pod -> pod.status()).setHeader("Status").setSortProperty("status");
        grid.addColumn(pod -> pod.age()).setHeader("Age").setSortProperty("age");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        grid.setDataProvider(new PodProvider());
        grid.addSelectionListener(event -> {
            actions.forEach(a -> a.update(event.getAllSelectedItems()));
        });
        grid.addItemClickListener(event -> {
            if (event.getClickCount() == 1) {
                if (event.isAltKey()) {
                    if (grid.getSelectionModel().isSelected(event.getItem()))
                        grid.getSelectionModel().deselect(event.getItem());
                    else
                        grid.getSelectionModel().select(event.getItem());
                } else
                if (event.isShiftKey()) {
                    var first = grid.getSelectionModel().getFirstSelectedItem();
                    if (first == null) {
                        grid.getSelectionModel().select(event.getItem());
                    } else {
                        var start = filteredList.indexOf(first.get());
                        var end = filteredList.indexOf(event.getItem());
                        if (start > end) {
                            var tmp = start;
                            start = end;
                            end = tmp;
                        }
                        grid.getSelectionModel().deselectAll();
                        for (int i = start; i <= end; i++) {
                            grid.getSelectionModel().select(filteredList.get(i));
                        }
                    }
                } else {
                    if (grid.getSelectionModel().isSelected(event.getItem()))
                        grid.getSelectionModel().deselectAll();
                    else {
                        grid.getSelectionModel().deselectAll();
                        grid.getSelectionModel().select(event.getItem());
                    }
                }
            }
        });
    }

    @Override
    public void setFilter(String value) {
        filterText = value;
        if (podList != null)
            grid.getDataProvider().refreshAll();
    }

    @Override
    public void setNamespace(String value) {
        namespace = value;
        if (podList != null) {
            podList = null;
            grid.getDataProvider().refreshAll();
        }
    }

    @Override
    public void setResourceType(String resourceType) {

    }

    private class PodProvider extends CallbackDataProvider<Pod, Void> {

        public PodProvider() {
            super(query -> {
                        LOGGER.info("Do the query {}",query);
                        if (filteredList == null) return Stream.empty();
                        for(QuerySortOrder queryOrder :
                                query.getSortOrders()) {
                            Collections.sort(filteredList, (a, b) -> switch (queryOrder.getSorted()) {
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
                        return filteredList.stream().skip(query.getOffset()).limit(query.getLimit());
                    }, query -> {
                        LOGGER.info("Do the size query {}",query);
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
                                                    getAge(pod.getMetadata().getCreationTimestamp()),
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
                filteredList = podList.stream().filter(pod -> pod.name().matches(f)).collect(Collectors.toList());
            } else {
                filteredList = podList.stream().filter(pod -> pod.name().contains(filter)).collect(Collectors.toList());
            }
        }
    }

    public record Pod(
            String name,
            String namespace,
            String status,
            String age,
            long created,
            V1Pod pod
            ) {

    }

    @Getter
    @Setter
    private class MenuAction {
        XUiAction action;
        MenuItem menuItem;

        public void update(Set<Pod> selected) {
            menuItem.setEnabled(action.canHandleResource(K8sUtil.RESOURCE_PODS,
                    selected == null ? Collections.emptySet() : selected));
        }
        public void execute() {
            if (!action.canHandleResource(K8sUtil.RESOURCE_PODS, grid.getSelectedItems())) {
                Notification notification = Notification
                        .show("Can't execute");
                notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            final var context = ExecutionContext.builder()
                    .resourceType(K8sUtil.RESOURCE_PODS)
                    .selected(grid.getSelectedItems())
                    .namespace(namespace)
                    .api(coreApi)
                    .clusterConfiguration(clusterConfig)
                    .ui(UI.getCurrent())
                    .grid(PodGrid.this)
                    .mainView(view.getMainView())
                    .selectedTab(view.getXTab())
                    .build();
            try {
                action.execute(context);
            } catch (Exception e) {
                Notification notification = Notification
                        .show("Error\n"+e.getMessage());
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

        }
    }
}


