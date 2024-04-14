package de.mhus.kt2l.resources;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.MenuItemBase;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tree.IProperties;
import de.mhus.kt2l.cluster.ClusterConfiguration;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.mhus.commons.tools.MCollection.cropArray;

@Slf4j
public abstract class AbstractGrid<T, S extends Component> extends VerticalLayout implements ResourcesGrid {

    protected List<T> resourcesList = null;
    protected List<T> filteredList = null;
    private String filterText = "";
    protected String namespace;
    protected CoreV1Api coreApi;
    protected ClusterConfiguration.Cluster clusterConfig;
    protected Grid<T> resourcesGrid;
    private MenuBar menuBar;
    protected List<MenuAction> actions = new ArrayList<>(10);

    @Autowired
    private ActionService actionService;
    protected ResourcesGridPanel view;
    private Optional<T> selectedResource;
    protected S detailsComponent;
    private ResourcesFilter resourcesFilter;

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void refresh(long counter) {
        if (counter % 10 != 0) return;
        filterList();
        resourcesGrid.getDataProvider().refreshAll();
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
            add(menuBar, resourcesGrid, detailsComponent);
        else
            add(menuBar, resourcesGrid);
        setSizeFull();

        actions.forEach(a -> a.updateWithResources(Collections.emptySet()));

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

        actions.stream().sorted(Comparator.comparingInt((MenuAction a) -> a.getAction().getMenuOrder())).forEach(action -> {
            MenuItem item = createMenuBarItem(action.getAction());
            item.addClickListener((ComponentEventListener<ClickEvent<MenuItem>>) event -> action.execute());
            item.setEnabled(false);

            action.setMenuItem(item);

            if (action.getAction().getShortcutKey() != null) {
                final var k1 = action.getAction().getShortcutKey().split("\\+");
                final var modifierStrings = cropArray(k1, 0, k1.length-1);
                final var modifier = new KeyModifier[modifierStrings.length];
                for (int i = 0; i < modifierStrings.length; i++)
                    modifier[i] = KeyModifier.valueOf(modifierStrings[i].toUpperCase());
                final var key = Key.of(k1[k1.length-1].toLowerCase());
                if (key != null) {
                    var sl = UI.getCurrent().addShortcutListener(() -> {
                        action.execute();
                    }, key, modifier);
                    if (detailsComponent != null)
                        sl.listenOn(resourcesGrid, detailsComponent);
                    else
                        sl.listenOn(resourcesGrid);
                }
            }

        });

    }

    private MenuItem createMenuBarItem( ResourceAction action) {
        final var path = action.getMenuPath();
        final var actionName = MString.beforeIndexOrAll(action.getTitle(), ';') + (action.getShortcutKey() == null ? "" : "  [" + action.getShortcutKey() + "]" );
        final var actionDress = MString.afterIndex(action.getTitle(), ';');

        if (MString.isEmptyTrim(path))
            return dressUpMenuItem(menuBar.addItem(new Div(actionName)), action, false);

        MenuItem current = null;
        for (String part : path.split("/")) {
            part = part.trim();
            if (MString.isEmpty(part)) continue;
            final var partName = MString.beforeIndexOrAll(part, ';');
            final var dress = MString.afterIndex(part, ';');
            if (current == null) {
                current = menuBar.getItems().stream().filter(i -> i.getText().equalsIgnoreCase(partName)).findFirst().orElseGet(
                        () -> dressUpMenuItem(menuBar.addItem(partName), dress, false));
            } else {
                final var finalCurrent = current;
                current = finalCurrent.getSubMenu().getItems().stream().filter(i -> i.getText().equalsIgnoreCase(partName)).findFirst().orElseGet(
                        () -> dressUpMenuItem(finalCurrent.getSubMenu().addItem(partName), dress, true));
            }
        }

        if (current == null)
            return dressUpMenuItem(menuBar.addItem(new Div(actionName)), action, false);
        return dressUpMenuItem(current.getSubMenu().addItem(new Div(actionName)), actionDress, true);
    }

    protected <T> GridMenuItem<T>createContextMenuItem( GridContextMenu<T> menu, ResourceAction action) {
        final var path = action.getMenuPath();
        final var actionName = MString.beforeIndexOrAll(action.getTitle(), ';');

        if (MString.isEmptyTrim(path))
            return dressUpMenuItem(menu.addItem(actionName), action, true);

        GridMenuItem<T> current = null;
        for (String part : path.split("/")) {
            part = part.trim();
            if (MString.isEmpty(part)) continue;
            final var partName = MString.beforeIndexOrAll(part, ';');
            final var dress = MString.afterIndex(part, ';');
            if (current == null) {
                current = menu.getItems().stream().filter(i -> i.getText().equalsIgnoreCase(partName)).findFirst().orElseGet(
                        () -> dressUpMenuItem(menu.addItem(partName), dress, true));
            } else {
                final var finalCurrent = current;
                current = finalCurrent.getSubMenu().getItems().stream().filter(i -> i.getText().equalsIgnoreCase(partName)).findFirst().orElseGet(
                        () -> dressUpMenuItem(finalCurrent.getSubMenu().addItem(partName), dress, true));
            }
        }

        if (current == null)
            return dressUpMenuItem(menu.addItem(actionName), action, true);
        return dressUpMenuItem(current.getSubMenu().addItem(actionName), action, true);
    }

    private <T extends MenuItemBase> T dressUpMenuItem(T item, ResourceAction action, boolean isChild) {
        if (action.getDescription() != null)
            item.getElement().setAttribute("title", action.getDescription());
//        if (action.getShortcutKey() != null) {
//            var shortCutText = new Div(action.getShortcutKey());
//            //shortCutText.addClassName("shortcuttext");
//            item.add(shortCutText);
//        }
        final var actionDress = MString.afterIndex(action.getTitle(), ';');
        return dressUpMenuItem(item, actionDress, isChild);
    }

    private <T extends MenuItemBase> T dressUpMenuItem(T item, String dress, boolean isChild) {
        if (MString.isEmptyTrim(dress)) return item;
        final var properties = IProperties.toProperties(dress);
        if (properties.isProperty("icon")) {
            final var icon = new Icon(properties.getString("icon", VaadinIcon.QUESTION.name()).toLowerCase().replace('_', '-'));
            if (isChild) {
                icon.getStyle().set("width", "var(--lumo-icon-size-s)");
                icon.getStyle().set("height", "var(--lumo-icon-size-s)");
                icon.getStyle().set("marginRight", "var(--lumo-space-s)");
            }
            item.addComponentAsFirst(icon);
        }
        return item;
    }

    protected abstract void createDetailsComponent();

    private void createGrid() {

        resourcesGrid = new Grid<>(getManagedClass(), false);
        addClassNames("contact-grid");
        resourcesGrid.setSizeFull();
        resourcesGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        ((GridMultiSelectionModel)resourcesGrid.getSelectionModel()).setSelectAllCheckboxVisibility(GridMultiSelectionModel.SelectAllCheckboxVisibility.VISIBLE);
        createGridColumns(resourcesGrid);
        resourcesGrid.getColumns().forEach(col -> {
            col.setAutoWidth(true);
            col.setResizable(true);
        });
        resourcesGrid.setDataProvider(createDataProvider());

        resourcesGrid.addCellFocusListener(event -> {
            selectedResource = event.getItem();
            if (selectedResource.isPresent())
                onGridCellFocusChanged(selectedResource.get());
        });

        resourcesGrid.addSelectionListener(event -> {
            onGridSelectionChanged();
            actions.forEach(a -> a.updateWithResources(event.getAllSelectedItems()));
        });
        resourcesGrid.addItemClickListener(event -> {
            if (event.getClickCount() == 2) {
                onShowDetails(event.getItem(), true);
            } else
            if (event.getClickCount() == 1) {
                onDetailsChanged(event.getItem());
                if (event.isAltKey()) {
                    if (resourcesGrid.getSelectionModel().isSelected(event.getItem()))
                        resourcesGrid.getSelectionModel().deselect(event.getItem());
                    else
                        resourcesGrid.getSelectionModel().select(event.getItem());
                } else
                if (event.isShiftKey()) {
                    var first = resourcesGrid.getSelectionModel().getFirstSelectedItem();
                    if (first == null) {
                        resourcesGrid.getSelectionModel().select(event.getItem());
                    } else {
                        var start = filteredList.indexOf(first.get());
                        var end = filteredList.indexOf(event.getItem());
                        if (start > end) {
                            var tmp = start;
                            start = end;
                            end = tmp;
                        }
                        resourcesGrid.getSelectionModel().deselectAll();
                        for (int i = start; i <= end; i++) {
                            resourcesGrid.getSelectionModel().select(filteredList.get(i));
                        }
                    }
                } else {
                    if (resourcesGrid.getSelectionModel().isSelected(event.getItem()))
                        resourcesGrid.getSelectionModel().deselectAll();
                    else {
                        resourcesGrid.getSelectionModel().deselectAll();
                        resourcesGrid.getSelectionModel().select(event.getItem());
                    }
                }
            }
        });


        GridContextMenu<T> menu = resourcesGrid.addContextMenu();
        actions.stream().sorted(Comparator.comparingInt((MenuAction a) -> a.getAction().getMenuOrder())).forEach(action -> {
            var item = createContextMenuItem(menu, action.getAction());
            item.addMenuItemClickListener(event -> action.execute());
            action.setContextMenuItem(item);
        });

        UI.getCurrent().addShortcutListener(this::handleShortcut, Key.SPACE).listenOn(resourcesGrid);
        UI.getCurrent().addShortcutListener(this::handleShortcut, Key.ENTER).listenOn(resourcesGrid);

    }

    protected abstract void onDetailsChanged(T item);

    protected abstract void onShowDetails(T item, boolean flip);

    protected abstract void onGridSelectionChanged();

    protected abstract Class<T> getManagedClass();

    protected abstract void onGridCellFocusChanged(T t);

    protected abstract DataProvider<T,?> createDataProvider();

    protected abstract void createGridColumns(Grid<T> resourcesGrid);

    @Override
    public void setFilter(String value, ResourcesFilter filter) {
        filterText = value;
        resourcesFilter = filter;
        // filterList(); //XXX ???
        if (resourcesList != null)
            resourcesGrid.getDataProvider().refreshAll();
    }

    @Override
    public void setNamespace(String value) {
        namespace = value;
        if (resourcesList != null) {
            resourcesList = null;
            resourcesGrid.getDataProvider().refreshAll();
        }
    }

    @Override
    public void setResourceType(String resourceType) {

    }

    @Override
    public void handleShortcut(ShortcutEvent event) {
        if (event.getKey().matches(" ") && event.getKeyModifiers().size() == 0) {
            if (selectedResource != null && selectedResource.isPresent()) {
                if (resourcesGrid.getSelectionModel().isSelected(selectedResource.get()))
                    resourcesGrid.getSelectionModel().deselect(selectedResource.get());
                else
                    resourcesGrid.getSelectionModel().select(selectedResource.get());
            }
            return;
        }
        if (event.getKey().matches(Key.ENTER.toString()) && event.getKeyModifiers().size() == 0) {
            if (selectedResource != null && selectedResource.isPresent()) {
                onShowDetails(selectedResource.get(), false);
            }
            return;
        }

    }

    @Override
    public void setSelected() {

        resourcesGrid.getElement().getNode()
                .runWhenAttached(ui -> ui.getPage().executeJs(
                        "setTimeout(function(){let firstTd = $0.shadowRoot.querySelector('tr:first-child > td:first-child'); firstTd.click(); firstTd.focus(); },0)", resourcesGrid.getElement()));

    }

    @Override
    public void setUnselected() {
    }

    protected void filterList() {
        if (resourcesList == null) {
            filteredList = Collections.emptyList();
        } else {
            List<T> list;
            if (resourcesFilter != null) {
                list = resourcesList.stream().filter(res -> resourcesFilter.filter(getSelectedKubernetesObject(res))).collect(Collectors.toList());
            } else {
                list = resourcesList;
            }
            final var filter = filterText;
            if (filter.isBlank()) {
                filteredList = list;
            } if (filter.startsWith("/")) {
                var f = filter.substring(1);
                filteredList = list.stream().filter(res -> filterByRegex(res, f)).collect(Collectors.toList());
            } else {
                filteredList = list.stream().filter(res -> filterByContent(res, filter)).collect(Collectors.toList());
            }
        }
    }

    protected abstract boolean filterByContent(T resource, String filter);

    protected abstract boolean filterByRegex(T resource, String filter);


    @Getter
    @Setter
    public class MenuAction {
        ResourceAction action;
        MenuItem menuItem;
        GridMenuItem<T> contextMenuItem;

        public void disableMenu() {
            if (menuItem != null)
                menuItem.setEnabled(false);
            if (contextMenuItem != null)
                contextMenuItem.setEnabled(false);
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
        public void updateWithResources(Set<T> selected) {
            var enabled = action.canHandleResource(getManagedResourceType(),
                    selected == null ? Collections.emptySet() : selected.stream().map(p -> getSelectedKubernetesObject(p)).collect(Collectors.toSet()));
            if (menuItem != null)
                menuItem.setEnabled(enabled);
            if (contextMenuItem != null)
                contextMenuItem.setEnabled(enabled);
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
                if (!action.canHandleResource(getManagedResourceType(), resourcesGrid.getSelectedItems().stream().map(p -> getSelectedKubernetesObject(p)).collect(Collectors.toSet()) )) {
                    Notification notification = Notification
                            .show("Can't execute");
                    notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
                    return;
                }
                context = ExecutionContext.builder()
                        .resourceType(getManagedResourceType())
                        .selected(resourcesGrid.getSelectedItems().stream().map(p -> getSelectedKubernetesObject(p)).collect(Collectors.toSet()))
                        .namespace(namespace)
                        .api(coreApi)
                        .clusterConfiguration(clusterConfig)
                        .ui(UI.getCurrent())
                        .grid(AbstractGrid.this)
                        .mainView(view.getMainView())
                        .selectedTab(view.getXTab())
                        .build();
//            }
            execute (context);

        }

        public void execute(ExecutionContext context) {
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

    protected abstract KubernetesObject getSelectedKubernetesObject(T resource);
}


