package de.mhus.kt2l.resources;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import de.mhus.kt2l.generic.ActionService;
import de.mhus.kt2l.cluster.ClusterConfiguration;
import de.mhus.kt2l.generic.ExecutionContext;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.generic.ResourceAction;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.mhus.commons.tools.MCollection.cropArray;

@Slf4j
public abstract class AbstractGrid<T, S extends Component> extends VerticalLayout implements ResourcesGrid {

    protected List<T> podList = null;
    protected List<T> filteredList = null;
    private String filterText = "";
    protected String namespace;
    protected CoreV1Api coreApi;
    protected ClusterConfiguration.Cluster clusterConfig;
    protected Grid<T> podGrid;
    private MenuBar menuBar;
    private List<MenuAction> actions = new ArrayList<>(10);

    @Autowired
    private ActionService actionService;
    protected ResourcesGridPanel view;
    private Optional<T> selectedPod;
    protected S detailsComponent;

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

        createActions();
        createGrid();
        createDetailsComponent();
        createMenuBar();

        if (detailsComponent != null)
            add(menuBar, podGrid, detailsComponent);
        else
            add(menuBar, podGrid);
        setSizeFull();

        actions.forEach(a -> a.updateWithPod(Collections.emptySet()));

        init();

    }

    protected abstract void init();

    public abstract String getManagedResourceType();

    private void createActions() {
        actionService.findActionsForResource(getManagedResourceType()).forEach(action -> {
            final MenuAction menuAction = new MenuAction();
            menuAction.setAction(action);
            actions.add(menuAction);
        });
    }

    private void createMenuBar() {
        menuBar = new MenuBar();

        actions.forEach(action -> {

            MenuItem item = menuBar.addItem(action.getAction().getTitle(), new ComponentEventListener<ClickEvent<MenuItem>>() {
                @Override
                public void onComponentEvent(ClickEvent<MenuItem> event) {
                    action.execute();
                }
            });
            item.setEnabled(false);
            item.getElement().setAttribute("title", action.getAction().getDescription() + (action.getAction().getShortcutKey() == null ? "" : " (" + action.getAction().getShortcutKey() + ")" ));

            action.setMenuItem(item);

            if (action.getAction().getShortcutKey() != null) {
                final var k1 = action.getAction().getShortcutKey().split("\\+");
                final var key = Key.of(k1[k1.length-1], cropArray(k1, 0, k1.length-1));
                if (key != null) {
                    var sl = UI.getCurrent().addShortcutListener(() -> {
                        action.execute();
                    },key);
                    if (detailsComponent != null)
                        sl.listenOn(podGrid, detailsComponent);
                    else
                        sl.listenOn(podGrid);
                }
            }

        });

    }
    protected abstract void createDetailsComponent();

    private void createGrid() {

        podGrid = new Grid<>(getManagedClass(), false);
        addClassNames("contact-grid");
        podGrid.setSizeFull();
        podGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        createGridColumns(podGrid);
        podGrid.getColumns().forEach(col -> col.setAutoWidth(true));
        podGrid.setDataProvider(createDataProvider());

        podGrid.addCellFocusListener(event -> {
            selectedPod = event.getItem();
            if (selectedPod.isPresent())
                onGridCellFocusChanged(selectedPod.get());
        });

        podGrid.addSelectionListener(event -> {
            onGridSelectionChanged();
            actions.forEach(a -> a.updateWithPod(event.getAllSelectedItems()));
        });
        podGrid.addItemClickListener(event -> {
            if (event.getClickCount() == 2) {
                onShowDetails(event.getItem(), true);
            } else
            if (event.getClickCount() == 1) {
                onDetailsChanged(event.getItem());
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


        GridContextMenu<T> menu = podGrid.addContextMenu();
        actions.forEach(action -> {
            var item = menu.addItem(action.getAction().getTitle(), event ->
                    action.execute()
            );
            action.setPodContextMenuItem(item);
        });

        UI.getCurrent().addShortcutListener(this::handleShortcut, Key.SPACE).listenOn(podGrid);
        UI.getCurrent().addShortcutListener(this::handleShortcut, Key.ENTER).listenOn(podGrid);

    }

    protected abstract void onDetailsChanged(T item);

    protected abstract void onShowDetails(T item, boolean flip);

    protected abstract void onGridSelectionChanged();

    protected abstract Class<T> getManagedClass();

    protected abstract void onGridCellFocusChanged(T t);

    protected abstract DataProvider<T,?> createDataProvider();

    protected abstract void createGridColumns(Grid<T> podGrid);

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
                onShowDetails(selectedPod.get(), false);
            }
            return;
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

    protected void filterList() {
        if (podList == null) {
            filteredList = Collections.emptyList();
        } else {
            final var filter = filterText;
            if (filter.isBlank()) {
                filteredList = podList;
            } if (filter.startsWith("/")) {
                var f = filter.substring(1);
                filteredList = podList.stream().filter(pod -> filterByRegex(pod, f)).collect(Collectors.toList());
            } else {
                filteredList = podList.stream().filter(pod -> filterByContent(pod, filter)).collect(Collectors.toList());
            }
        }
    }

    protected abstract boolean filterByContent(T pod, String filter);

    protected abstract boolean filterByRegex(T pod, String filter);


    @Getter
    @Setter
    private class MenuAction {
        ResourceAction action;
        MenuItem menuItem;
        GridMenuItem<T> podContextMenuItem;
//        GridMenuItem<Container> containerContextMenuItem;

        public void disableMenu() {
            if (menuItem != null)
                menuItem.setEnabled(false);
            if (podContextMenuItem != null)
                podContextMenuItem.setEnabled(false);
//            if (containerContextMenuItem != null)
//                containerContextMenuItem.setEnabled(false);
        }

//        public void updateWithContainer(Set<Container> selected) {
//            var enabled = action.canHandleResource(K8sUtil.RESOURCE_CONTAINER,
//                    selected == null ? Collections.emptySet() : selected);
//            if (menuItem != null)
//                menuItem.setEnabled(enabled);
//            if (podContextMenuItem != null)
//                podContextMenuItem.setEnabled(enabled);
//            if (containerContextMenuItem != null)
//                containerContextMenuItem.setEnabled(enabled);
//        }
        public void updateWithPod(Set<T> selected) {
            var enabled = action.canHandleResource(K8sUtil.RESOURCE_PODS,
                    selected == null ? Collections.emptySet() : selected);
            if (menuItem != null)
                menuItem.setEnabled(enabled);
            if (podContextMenuItem != null)
                podContextMenuItem.setEnabled(enabled);
//            if (containerContextMenuItem != null)
//                containerContextMenuItem.setEnabled(enabled);
        }
        public void execute() {
            ExecutionContext context = null;
//            if (containerGrid.isVisible() && containerGrid.getSelectedItems() != null && containerGrid.getSelectedItems().size() == 1) {
//
//                if (!action.canHandleResource(K8sUtil.RESOURCE_CONTAINER, containerGrid.getSelectedItems())) {
//                    Notification notification = Notification
//                            .show("Can't execute");
//                    notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
//                    return;
//                }
//
//                context = ExecutionContext.builder()
//                        .resourceType(K8sUtil.RESOURCE_CONTAINER)
//                        .selected(containerGrid.getSelectedItems())
//                        .namespace(namespace)
//                        .api(coreApi)
//                        .clusterConfiguration(clusterConfig)
//                        .ui(UI.getCurrent())
//                        .grid(PodGrid.this)
//                        .mainView(view.getMainView())
//                        .selectedTab(view.getXTab())
//                        .build();
//
//            } else {
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
                        .grid(AbstractGrid.this)
                        .mainView(view.getMainView())
                        .selectedTab(view.getXTab())
                        .build();
//            }

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


