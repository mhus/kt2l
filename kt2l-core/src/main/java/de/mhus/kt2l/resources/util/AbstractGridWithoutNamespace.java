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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.commons.lang.IRegistration;
import de.mhus.commons.tools.MLang;
import de.mhus.commons.tools.MObject;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.ResourcesGrid;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.util.Watch;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static de.mhus.commons.tools.MLang.tryThis;

@Configurable
@Slf4j
public abstract class AbstractGridWithoutNamespace<T extends AbstractGridWithoutNamespace.ResourceItem<V>, S extends Component,V extends KubernetesObject, L extends KubernetesListObject> extends AbstractGrid<T, S>{

    private IRegistration eventRegistration;
    @Autowired
    protected K8sService k8sService;
    protected HandlerK8s resourceHandler;

    @Override
    protected void init() {
        this.eventRegistration = ((AbstractClusterWatch<KubernetesObject>)panel.getCore().backgroundJobInstance(
                panel.getCluster(),
                getManagedWatchClass()
        )).getEventHandler().registerWeak(this::changeEvent);
        this.resourceHandler = k8sService.getTypeHandler(getManagedType());
    }

    protected abstract Class<? extends ClusterBackgroundJob> getManagedWatchClass();

    private void changeEvent(Watch.Response<KubernetesObject> event) {
        if (resourcesList == null) return;

        if (event.type.equals(K8sUtil.WATCH_EVENT_ADDED) || event.type.equals(K8sUtil.WATCH_EVENT_MODIFIED)) {
            synchronized (this) {
                AtomicBoolean added = new AtomicBoolean(false);
                final var foundRes = MLang.synchronize(() -> resourcesList.stream().filter(res -> res.getName().equals(event.object.getMetadata().getName())).findFirst().orElseGet(
                        () -> {
                            final var res = createResourceItem();
                            res.initResource((V) event.object, true, AbstractGridWithoutNamespace.this);
                            res.updateResource();
                            resourcesList.add((T) res);
                            added.set(true);
                            return (T) res;
                        }
                ));

                foundRes.setResource((V) event.object);
                foundRes.updateResource();
                filterList();
                if (added.get())
                    getPanel().getCore().ui().access(() -> resourcesGrid.getDataProvider().refreshAll());
                else
                    getPanel().getCore().ui().access(() -> resourcesGrid.getDataProvider().refreshItem(foundRes));
            }
        } else
        if (event.type.equals(K8sUtil.WATCH_EVENT_DELETED)) {
            AtomicBoolean removed = new AtomicBoolean(false);
            synchronized (resourcesList) {
                resourcesList.removeIf(res -> {
                    if (res.equals(event.object)) {
                        removed.set(true);
                        return true;
                    }
                    return false;
                });
            }
            if (removed.get()) {
                filterList();
                getPanel().getCore().ui().access(() -> resourcesGrid.getDataProvider().refreshAll());
            }
        }

    }

    protected abstract T createResourceItem();

    @Override
    public abstract V1APIResource getManagedType();

    @Override
    protected void createDetailsComponent() {

    }

    @Override
    protected void onDetailsChanged(T item) {

    }

    @Override
    protected void onShowDetails(T item, boolean flip) {

    }

    @Override
    protected void onGridSelectionChanged() {

    }

    @Override
    protected abstract Class<T> getManagedResourceItemClass();

    @Override
    protected void onGridCellFocusChanged(T resource) {

    }

    @Override
    protected void fillStatusLine(StringBuilder sb) {
    }

    @Override
    protected DataProvider<T, ?> createDataProvider() {
        return (DataProvider<T, ?>) new ResourceDataProvider();
    }

    @Override
    protected void createGridColumns(Grid<T> resourcesGrid) {
        resourcesGrid.addColumn(res -> res.getName()).setHeader("Name").setSortProperty("name");
        createGridColumnsAfterName(resourcesGrid);
        resourcesGrid.addColumn(res -> res.getAge()).setHeader("Age").setSortProperty("age");
        if (viewConfig.getBoolean("showResourceVersion", false))
            resourcesGrid.addColumn(res -> res.getResourceVersion()).setHeader("RVersion").setSortProperty("resourceversion");
        createGridColumnsAtEnd(resourcesGrid);
    }

    protected void createGridColumnsAtEnd(Grid<T> resourcesGrid) {

    }

    protected abstract void createGridColumnsAfterName(Grid<T> resourcesGrid);

    @Override
    protected boolean filterByContent(T resource, String filter) {
        return resource.getName().contains(filter);
    }

    @Override
    protected boolean filterByRegex(T resource, String filter) {
        return resource.getName().matches(filter);
    }

    @Override
    protected KubernetesObject getSelectedKubernetesObject(T resource) {
        return resource.getResource();
    }

    @Override
    public void destroy() {
        if (eventRegistration != null)
            eventRegistration.unregister();
        super.destroy();
    }

    @Override
    public boolean isNamespaced() {
        return false;
    }

    public class ResourceDataProvider extends CallbackDataProvider<ResourceItem<V>, Void> {
        public ResourceDataProvider() {
            super(query -> {
                synchronized (AbstractGridWithoutNamespace.this) {
                    LOGGER.debug("◌ Do the query {} {}", getManagedType().getName(), queryToString(query));
                    if (filteredList == null) return Stream.empty();
                    for (QuerySortOrder queryOrder :
                            query.getSortOrders()) {
                        Collections.sort(filteredList, (a, b) -> switch (queryOrder.getSorted()) {
                            case "name" -> switch (queryOrder.getDirection()) {
                                case ASCENDING -> a.getName().compareTo(b.getName());
                                case DESCENDING -> b.getName().compareTo(a.getName());
                            };
                            case "age" -> switch (queryOrder.getDirection()) {
                                case ASCENDING -> K8sUtil.compareTo(a.getCreated(), b.getCreated());
                                case DESCENDING -> K8sUtil.compareTo(b.getCreated(), a.getCreated());
                            };
                            case "resourceversion" -> switch (queryOrder.getDirection()) {
                                case ASCENDING -> MObject.compareTo(a.getResourceVersion(), b.getResourceVersion());
                                case DESCENDING -> MObject.compareTo(b.getResourceVersion(), a.getResourceVersion());
                            };
                            default -> sortColumn(queryOrder.getSorted(), queryOrder.getDirection(), a, b);
                        });

                    }
                    return (Stream<ResourceItem<V>>) filteredList.stream().skip(query.getOffset()).limit(query.getLimit());
                }
            }, query -> {
                if (resourcesList == null) {
                    resourcesList = new ArrayList<>();
                    synchronized (AbstractGridWithoutNamespace.this) {
                        tryThis(() -> createResourceListWithoutNamespace())
                                .onFailure(e -> LOGGER.error("Can't fetch resources from cluster", e))
                                .onSuccess(list -> {
                                    list.getItems().forEach(res -> {
                                        var newRes = createResourceItem();
                                        newRes.initResource((V)res, false, AbstractGridWithoutNamespace.this);
                                        newRes.updateResource();
                                        resourcesList.add(newRes);
                                    });
                                });
                    }
                }
                filterList();
                var res = filteredList.size();
                LOGGER.debug("◌ The size query {} {} returns {}", getManagedType().getName(), queryToString(query), res);
                return res;
            });
        }

    }

    protected String getGridRowClass(T res) {
        return res.color != null ? res.color.name().toLowerCase() : null;
    }

    public void refresh(long counter) {
        super.refresh(counter);
        if (counter % 2 == 0) {
            if (resourcesList == null) return;
            synchronized (resourcesList) {
                resourcesList.forEach(res -> {
                    if (res.color != null && res.colorTimeout != 0 && res.colorTimeout < System.currentTimeMillis()) {
                        res.color = res.altColor;
                        res.colorTimeout = 0;
                        getPanel().getCore().ui().access(() -> {
                            resourcesGrid.getDataProvider().refreshItem(res);
                        });
                    }
                });
            }
        }
    }

    protected abstract int sortColumn(String sorted, SortDirection direction, T a, T b);

    protected L createResourceListWithoutNamespace() throws ApiException {
        return resourceHandler.createResourceListWithoutNamespace(cluster.getApiProvider());
    }

    @Getter
    public static class ResourceItem<V extends KubernetesObject> {
        protected UiUtil.COLOR altColor;
        protected UiUtil.COLOR color;
        protected long colorTimeout;
        protected String name;
        protected OffsetDateTime created;
        protected V resource;
        private ResourcesGrid grid;
        private String resourceVersion;

        void initResource(V resource, boolean newResource, ResourcesGrid grid) {
            this.name = resource.getMetadata().getName();
            this.resource = resource;
            this.grid = grid;
            if (newResource) {
                color = UiUtil.COLOR.GREEN;
                colorTimeout = System.currentTimeMillis() + 2000;
            }
        }

        public void updateResource() {
            this.created = resource.getMetadata().getCreationTimestamp();
            this.resourceVersion = tryThis(() -> resource.getMetadata().getResourceVersion()).orElse("");
        }

        public String getAge() {
            return K8sUtil.getAge(resource.getMetadata().getCreationTimestamp());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (o instanceof ResourceItem res)
                return Objects.equals(name, res.name);
            if (o instanceof KubernetesObject res)
                return Objects.equals(name, res.getMetadata().getName());
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        void setResource(V object) {
            this.resource = object;
        }

        public void setFlashColor(UiUtil.COLOR color) {
            if (colorTimeout != 0) return;
            this.color = color;
            this.colorTimeout = System.currentTimeMillis() + 2000;
        }

        public void setColor(UiUtil.COLOR color) {
            this.altColor = color;
            if (colorTimeout == 0)
                this.color = color;
        }

    }

}
