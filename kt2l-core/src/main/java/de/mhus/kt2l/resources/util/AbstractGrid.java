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

package de.mhus.kt2l.resources.util;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.MenuItemBase;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.commons.tools.MCollection;
import de.mhus.commons.tools.MObject;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tree.IProperties;
import de.mhus.commons.tree.ITreeNode;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.ViewsConfiguration;
import de.mhus.kt2l.resources.ActionService;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.resources.ResourcesFilter;
import de.mhus.kt2l.resources.ResourcesGrid;
import de.mhus.kt2l.resources.ResourcesGridPanel;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
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

import static de.mhus.commons.tools.MString.isEmpty;

@Slf4j
public abstract class AbstractGrid<T, S extends Component> extends VerticalLayout implements ResourcesGrid {

    protected volatile List<T> resourcesList = null;
    @Getter // for testing
    protected volatile List<T> filteredList = null;
    private String filterText = "";
    protected String namespace;
    protected Cluster cluster;
    @Getter // for testing
    protected Grid<T> resourcesGrid;
    private MenuBar menuBar;
    protected List<MenuAction> actions = new ArrayList<>(10);

    @Autowired
    private ActionService actionService;
    @Autowired
    private ViewsConfiguration viewsConfiguration;

    @Getter
    protected ResourcesGridPanel panel;
    private Optional<T> selectedResource;
    protected S detailsComponent;
    private ResourcesFilter resourcesFilter;

    protected Div resourceAmount;
    protected Div selectedAmount;
    protected ITreeNode viewConfig;
    private SplitLayout detailsSplit;
    private List<ShortcutRegistration> shortcutRegistrations = Collections.synchronizedList(new ArrayList<>());

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void refresh(long counter) {
//        if (counter % 10 != 0) return;
//        filterList();
//        resourcesGrid.getDataProvider().refreshAll();
//        panel.getCore().ui().push();
    }

    @Override
    public void init(Cluster cluster, ResourcesGridPanel panel) {
        this.panel = panel;
        this.cluster = cluster;
        this.viewConfig = viewsConfiguration.getConfig("resourcesGrid");
//        if (namespace == null)
//            namespace = cluster.getDefaultNamespace();

        createActions();
        createGrid();
        try {
            createDetailsComponent();
        } catch (Exception e) {
            LOGGER.error("Error creating details component", e);
            detailsComponent = null;
        }
        createMenuBar();

        addAttachListener(event -> {
            setShortcuts();
        });

        var menuBarSpace = new Div();
        menuBarSpace.addClassName("div-space");
        if (detailsComponent != null) {
            detailsSplit = new SplitLayout(resourcesGrid, detailsComponent);
            detailsSplit.setSizeFull();
            detailsSplit.setOrientation(SplitLayout.Orientation.VERTICAL);
            add(MCollection.notNull(menuBar, menuBarSpace, detailsSplit, getFooter()));
        } else {
            add(MCollection.notNull(menuBar, menuBarSpace, resourcesGrid, getFooter()));
        }

        setSizeFull();

        actions.forEach(a -> a.updateWithResources(Collections.emptySet()));

        try {
            init();
        } catch (Exception e) {
            LOGGER.error("Error initializing", e);
        }

    }

    private HorizontalLayout getFooter() {

        resourceAmount = new Div();
        resourceAmount.addClassName("res-amount");
        resourceAmount.setText("-");

        selectedAmount = new Div();
        selectedAmount.addClassName("sel-amount");
        selectedAmount.setText("");

        HorizontalLayout footer = new HorizontalLayout(resourceAmount, selectedAmount);
        footer.addClassName("footer");
        footer.setPadding(false);
        footer.setMargin(false);
        return footer;

    }


    public void destroy() {
    }

    protected abstract void init();

    public abstract V1APIResource getManagedType();

    private void createActions() {
        try {
            actionService.findActionsForResource(cluster, getManagedType()).forEach(action -> {
                final MenuAction menuAction = new MenuAction();
                menuAction.setAction(action);
                actions.add(menuAction);
            });
        } catch (Exception e) {
            LOGGER.error("Error creating actions", e);
        }
    }

    private void createMenuBar() {
        menuBar = new MenuBar();
        try {
            actions.stream().sorted(Comparator.comparingInt((MenuAction a) -> a.getAction().getMenuOrder())).forEach(action -> {
                MenuItem item = createMenuBarItem(action.getAction());
                item.addClickListener((ComponentEventListener<ClickEvent<MenuItem>>) event -> action.execute());
                item.setEnabled(false);

                action.setMenuItem(item);

            });
        } catch (Exception e) {
            LOGGER.error("Error creating menu bar", e);
        }
    }

    private MenuItem createMenuBarItem( ResourceAction action) {
        final var path = action.getMenuPath();
//        final var actionName = MString.beforeIndexOrAll(action.getTitle(), ';') + "  " + UiUtil.toShortcutString(action.getShortcutKey());
        final var actionDress = MString.afterIndex(action.getTitle(), ';');
        final var icon = action.getIcon();

        if (MString.isEmptyTrim(path)) {
            return dressUpMenuItem(menuBar.addItem(createMenuBarTitle(action)), action, icon, false);
        }
        MenuItem current = null;
        for (String part : path.split("/")) {
            part = part.trim();
            if (isEmpty(part)) continue;
            final var partName = MString.beforeIndexOrAll(part, ';');
            final var dress = MString.afterIndex(part, ';');
            if (current == null) {
                current = menuBar.getItems().stream().filter(i -> i.getText().equalsIgnoreCase(partName)).findFirst().orElseGet(
                        () -> dressUpMenuItem(menuBar.addItem(partName), dress, null, false));
            } else {
                final var finalCurrent = current;
                current = finalCurrent.getSubMenu().getItems().stream().filter(i -> i.getText().equalsIgnoreCase(partName)).findFirst().orElseGet(
                        () -> dressUpMenuItem(finalCurrent.getSubMenu().addItem(partName), dress, null, true));
            }
        }

        if (current == null)
            return dressUpMenuItem(menuBar.addItem(createMenuBarTitle(action)), action, icon, false);
        return dressUpMenuItem(current.getSubMenu().addItem(createMenuBarTitle(action)), actionDress, icon, true);
    }

    private Component createMenuBarTitle(ResourceAction action) {
        final var actionName = MString.beforeIndexOrAll(action.getTitle(), ';');
        if (action.getShortcutKey() == null)
            return new Div(actionName);
        final var shortcut = UiUtil.toShortcutString(action.getShortcutKey());
        var shortcutSpan = new Span(shortcut);
        shortcutSpan.addClassName("shortcut");
        var titleSpan = new Span(actionName);
        titleSpan.addClassName("title");
        var titleDiv = new Div(titleSpan, shortcutSpan);
        titleDiv.addClassName("menutitle");
        return titleDiv;
    }

    protected <T> GridMenuItem<T>createContextMenuItem( GridContextMenu<T> menu, ResourceAction action) {
        final var path = action.getMenuPath();
        final var actionName = MString.beforeIndexOrAll(action.getTitle(), ';');
        final var icon = action.getIcon();

        if (MString.isEmptyTrim(path))
            return dressUpMenuItem(menu.addItem(actionName), action, icon, true);

        GridMenuItem<T> current = null;
        for (String part : path.split("/")) {
            part = part.trim();
            if (isEmpty(part)) continue;
            final var partName = MString.beforeIndexOrAll(part, ';');
            final var dress = MString.afterIndex(part, ';');
            if (current == null) {
                current = menu.getItems().stream().filter(i -> i.getText().equalsIgnoreCase(partName)).findFirst().orElseGet(
                        () -> dressUpMenuItem(menu.addItem(partName), dress, null, true));
            } else {
                final var finalCurrent = current;
                current = finalCurrent.getSubMenu().getItems().stream().filter(i -> i.getText().equalsIgnoreCase(partName)).findFirst().orElseGet(
                        () -> dressUpMenuItem(finalCurrent.getSubMenu().addItem(partName), dress, null, true));
            }
        }

        if (current == null)
            return dressUpMenuItem(menu.addItem(actionName), action, icon, true);
        return dressUpMenuItem(current.getSubMenu().addItem(actionName), action, icon, true);
    }

    private <T extends MenuItemBase> T dressUpMenuItem(T item, ResourceAction action, AbstractIcon icon, boolean isChild) {
        if (action.getDescription() != null)
            item.getElement().setAttribute("title", action.getDescription());
        final var actionDress = MString.afterIndex(action.getTitle(), ';');
        return dressUpMenuItem(item, actionDress, icon, isChild);
    }

    private <T extends MenuItemBase> T dressUpMenuItem(T item, String dress, AbstractIcon icon, boolean isChild) {
        if (MString.isEmptyTrim(dress)) return item;
        final var properties = IProperties.toProperties(dress);
        if (icon != null || properties.isProperty("icon")) {
            if (icon == null)
                icon = new Icon(properties.getString("icon", VaadinIcon.QUESTION.name()).toLowerCase().replace('_', '-'));
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

        resourcesGrid = new Grid<>(getManagedResourceItemClass(), false);
        addClassNames("contact-grid");
        setSpacing(false);
        resourcesGrid.setSizeFull();
        resourcesGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        try {
            ((GridMultiSelectionModel) resourcesGrid.getSelectionModel()).setSelectAllCheckboxVisibility(GridMultiSelectionModel.SelectAllCheckboxVisibility.VISIBLE);
            createGridColumns(resourcesGrid);
            resourcesGrid.getColumns().forEach(col -> {
                col.setAutoWidth(true);
                col.setResizable(true);
                col.setKey(col.getHeaderText().toLowerCase());
            });
            if (viewConfig.getBoolean("colors", true))
                resourcesGrid.setClassNameGenerator(this::getGridRowClass);
            resourcesGrid.setDataProvider(createDataProvider());

            resourcesGrid.addCellFocusListener(event -> {
                selectedResource = event.getItem();
                if (selectedResource.isPresent())
                    onGridCellFocusChanged(selectedResource.get());
            });

            resourcesGrid.addSelectionListener(event -> {
                var size = resourcesGrid.getSelectedItems().size();
                selectedAmount.setText(size == 0 ?  "" : "SEL: " + String.valueOf(size));
                onGridSelectionChanged();
                actions.forEach(a -> a.updateWithResources(event.getAllSelectedItems()));
            });
            resourcesGrid.addItemClickListener(event -> {
                if (event.getClickCount() == 2) {
                    onShowDetails(event.getItem(), true);
                } else if (event.getClickCount() == 1) {
                    onDetailsChanged(event.getItem());
                    if (event.isAltKey()) {
                        if (resourcesGrid.getSelectionModel().isSelected(event.getItem()))
                            resourcesGrid.getSelectionModel().deselect(event.getItem());
                        else
                            resourcesGrid.getSelectionModel().select(event.getItem());
                    } else if (event.isShiftKey()) {
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

            resourcesGrid.addSortListener(event -> {
                if (event.isFromClient())
                    panel.historyAdd();
            });

            GridContextMenu<T> menu = resourcesGrid.addContextMenu();
            actions.stream().sorted(Comparator.comparingInt((MenuAction a) -> a.getAction().getMenuOrder())).forEach(action -> {
                var item = createContextMenuItem(menu, action.getAction());
                item.addMenuItemClickListener(event -> action.execute());
                action.setContextMenuItem(item);
            });

        } catch (Exception e) {
            LOGGER.error("Error creating grid", e);
        }
    }


    protected void setShortcuts() {
        LOGGER.debug("Set shortcuts");
        shortcutRegistrations.forEach(ShortcutRegistration::remove);
        shortcutRegistrations.clear();

        addShortcutRegistration(getPanel().getCore().ui().addShortcutListener(this::handleShortcut, Key.SPACE).listenOn(resourcesGrid));
        addShortcutRegistration(getPanel().getCore().ui().addShortcutListener(this::handleShortcut, Key.ENTER).listenOn(resourcesGrid));

        addShortcutRegistration(getPanel().getCore().ui().addShortcutListener((event) -> panel.focusFilter() , Key.SLASH).listenOn(resourcesGrid));
        addShortcutRegistration(getPanel().getCore().ui().addShortcutListener((event) -> panel.focusResources() , Key.of(":")).withShift().listenOn(resourcesGrid));
        addShortcutRegistration(getPanel().getCore().ui().addShortcutListener((event) -> panel.focusNamespaces() , Key.QUOTE).listenOn(resourcesGrid));
        addShortcutRegistration(getPanel().getCore().ui().addShortcutListener((event) -> doRefreshGrid() , Key.KEY_R, UiUtil.getOSMetaModifier()).listenOn(resourcesGrid));

        actions.forEach(action -> {
            if (action.getAction().getShortcutKey() != null) {
                var shortcut = UiUtil.createShortcut(action.getAction().getShortcutKey());
                if (shortcut != null) {
                    LOGGER.debug("Shortcut: {} {}", action.getAction().getTitle(), shortcut);
                    shortcut.addShortcutListener(resourcesGrid, () -> {
                        action.execute();
                    });
                    addShortcutRegistration(shortcut.getRegistration());
                }
            }
        });
    }

    private void addShortcutRegistration(ShortcutRegistration shortcutRegistration) {
        shortcutRegistrations.add(shortcutRegistration);
    }

    protected void doRefreshGrid() {
        LOGGER.info("◌ Refresh Grid");
        cluster.getApiProvider().invalidate();
        resourcesList = null;
        resourcesGrid.getDataProvider().refreshAll();
    }

    protected String getGridRowClass(T res) {
        return null;
    }

    protected abstract void onDetailsChanged(T item);

    protected abstract void onShowDetails(T item, boolean flip);

    protected abstract void onGridSelectionChanged();

    protected abstract Class<T> getManagedResourceItemClass();

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
        if (MObject.equals(namespace, value)) return;
        namespace = value;
        if (resourcesList != null) {
            resourcesList = null;
            resourcesGrid.getDataProvider().refreshAll();
        }
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public void setType(V1APIResource type) {

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
                .runWhenAttached(ui -> getPanel().getCore().ui().getPage().executeJs(
                        "setTimeout(function(){let firstTd = $0.shadowRoot.querySelector('tr:first-child > td:first-child'); firstTd.click(); firstTd.focus(); },0)", resourcesGrid.getElement()));

    }

    @Override
    public void setUnselected() {
    }

    @Override
    public List<GridSortOrder<T>> getSortOrder() {
        return resourcesGrid.getSortOrder();
    }

    @Override
    public void setSortOrder(String sortOrder, boolean sortAscending) {
        if (isEmpty(sortOrder)) {
            resourcesGrid.sort(List.of());
            return;
        }
        resourcesGrid.sort(List.of( new GridSortOrder<>(resourcesGrid.getColumnByKey(sortOrder), sortAscending ? SortDirection.ASCENDING : SortDirection.DESCENDING)));
    }

    protected void filterList() {
        synchronized (this) {
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
                }
                if (filter.startsWith("/")) {
                    var f = filter.substring(1);
                    filteredList = list.stream().filter(res -> filterByRegex(res, f)).collect(Collectors.toList());
                } else {
                    filteredList = list.stream().filter(res -> filterByContent(res, filter)).collect(Collectors.toList());
                }
            }
        }
        getPanel().getCore().ui().access(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("RES: ");
            if (filteredList != null) {
                sb.append(filteredList.size());
                if (filteredList.size() != resourcesList.size())
                    sb.append(" / ").append(resourcesList.size());
            } else if (resourcesList != null)
                sb.append(resourcesList.size());
            else
                sb.append("-");
            fillStatusLine(sb);
            resourceAmount.setText(sb.toString());
        });
    }

    protected abstract void fillStatusLine(StringBuilder sb);

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
            var enabled = action.canHandleResource(cluster, getManagedType(),
                    selected == null ? Collections.emptySet() : selected.stream().map(p -> getSelectedKubernetesObject(p)).collect(Collectors.toSet()));
            if (menuItem != null)
                menuItem.setEnabled(enabled);
            if (contextMenuItem != null)
                contextMenuItem.setEnabled(enabled);
        }
        public void execute() {
            ExecutionContext context = null;

                final var selected = resourcesGrid.getSelectedItems().stream().map(p -> getSelectedKubernetesObject(p)).collect(Collectors.toSet());
                if (!action.canHandleResource(cluster, getManagedType(), selected )) {
                    UiUtil.showErrorNotification("Can't execute action");
                    return;
                }
                context = ExecutionContext.builder()
                        .type(getManagedType())
                        .selected(selected)
                        .namespace(namespace)
                        .cluster(cluster)
                        .ui(getPanel().getCore().ui())
                        .grid(AbstractGrid.this)
                        .core(panel.getCore())
                        .selectedTab(panel.getTab())
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

    protected <T, F> String queryToString(Query<T, F> query) {
        return "QUERY" +
                        (query.getOffset() == 0 && query.getLimit() == Integer.MAX_VALUE ?
                                "" : (" OFFSET " + query.getOffset() + " LIMIT " + query.getLimit())) +
                        (query.getSortOrders() == null || query.getSortOrders().isEmpty() ?
                                "" : (" ORDER " + query.getSortOrders().stream().map(s -> s.getSorted() + " " + s.getDirection()).collect(Collectors.joining(","))));
    }

}


