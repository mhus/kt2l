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
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.UiUtil;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.Watch;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public abstract class AbstractGridWithNamespace<T extends AbstractGridWithNamespace.ResourceItem<V>, S extends Component,V extends KubernetesObject, L extends KubernetesListObject> extends AbstractGrid<T, S>{

    private IRegistration eventRegistration;
    @Autowired
    protected K8sService k8sService;
    protected HandlerK8s resourceHandler;

    @Override
    protected void init() {
        resourceHandler = k8sService.getResourceHandler(getManagedResourceType());
        if (getManagedWatchClass() != null)
            eventRegistration = panel.getCore().backgroundJobInstance(panel.getCluster(), getManagedWatchClass()).getEventHandler().registerWeak(this::changeEvent);
    }

    protected abstract Class<? extends ClusterBackgroundJob> getManagedWatchClass();

    private void changeEvent(Watch.Response<KubernetesObject> event) {
        if (resourcesList == null) return;

        if (namespace != null && !namespace.equals(K8sUtil.NAMESPACE_ALL_LABEL) && !namespace.equals(event.object.getMetadata().getNamespace()))
            return;

        if (event.type.equals(K8sUtil.WATCH_EVENT_ADDED) || event.type.equals(K8sUtil.WATCH_EVENT_MODIFIED)) {

            AtomicBoolean added = new AtomicBoolean(false);
            final var foundRes = MLang.synchronize(() -> resourcesList.stream().filter(res -> res.getName().equals(event.object.getMetadata().getName())).findFirst().orElseGet(
                    () -> {
                        final var res = createResourceItem();
                        res.initResource((V)event.object, true);
                        res.updateResource();
                        resourcesList.add((T)res);
                        added.set(true);
                        return (T)res;
                    }
            ));

            foundRes.setResource((V)event.object);
            foundRes.updateResource();
            filterList();
            if (added.get())
                getPanel().getCore().ui().access(() -> resourcesGrid.getDataProvider().refreshAll());
            else
                getPanel().getCore().ui().access(() -> resourcesGrid.getDataProvider().refreshItem(foundRes));
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
    public abstract K8s getManagedResourceType();

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
    protected DataProvider<T, ?> createDataProvider() {
        return (DataProvider<T, ?>) new ResourceDataProvider();
    }

    @Override
    protected void createGridColumns(Grid<T> resourcesGrid) {
        resourcesGrid.addColumn(res -> res.getNamespace()).setHeader("Namespace").setSortProperty("namespace");
        resourcesGrid.addColumn(res -> res.getName()).setHeader("Name").setSortProperty("name");
        createGridColumnsAfterName(resourcesGrid);
        resourcesGrid.addColumn(res -> res.getAge()).setHeader("Age").setSortProperty("age");
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

    public class ResourceDataProvider extends CallbackDataProvider<ResourceItem<V>, Void> {
        public ResourceDataProvider() {
            super(query -> {
                        LOGGER.debug("◌ Do the query {}",query);
                        if (filteredList == null) return Stream.empty();
                        for(QuerySortOrder queryOrder :
                                query.getSortOrders()) {
                            Collections.sort(filteredList, (a, b) -> switch (queryOrder.getSorted()) {
                                case "name" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> a.getName().compareTo(b.getName());
                                    case DESCENDING -> b.getName().compareTo(a.getName());
                                };
                                case "namespace" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> a.getNamespace().compareTo(b.getNamespace());
                                    case DESCENDING -> b.getNamespace().compareTo(a.getNamespace());
                                };
                                case "age" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> Long.compare(a.getCreated(), b.getCreated());
                                    case DESCENDING -> Long.compare(b.getCreated(), a.getCreated());
                                };
                                default -> sortColumn(queryOrder.getSorted(), queryOrder.getDirection(), a, b);
                            });

                        }
                        return (Stream<ResourceItem<V>>) filteredList.stream().skip(query.getOffset()).limit(query.getLimit());
                    }, query -> {
                        LOGGER.debug("◌ Do the size query {}",query);
                        if (resourcesList == null) {
                            resourcesList = new ArrayList<>();
                            synchronized (resourcesList) {
                                tryThis(() -> namespace == null || namespace.equals(K8sUtil.NAMESPACE_ALL_LABEL) ? createRawResourceListForAllNamespaces() : createRawResourceListForNamespace(namespace) )
                                        .onFailure(e -> LOGGER.error("Can't fetch resources from cluster", e))
                                        .onSuccess(list -> {
                                            list.getItems().forEach(res -> {
                                                var newRes = createResourceItem();
                                                newRes.initResource((V) res, false);
                                                newRes.updateResource();
                                                resourcesList.add(newRes);
                                            });
                                        });
                            }
                        }
                        filterList();
                        return filteredList.size();
                    }
            );
        }

    }

    protected String getGridRowClass(T res) {
        return res.color != null ? res.color.name().toLowerCase() : null;
    }

    public void refresh(long counter) {
        super.refresh(counter);
        if (counter % 2 == 0) {
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


        protected L createRawResourceListForNamespace(String namespace) throws ApiException {
        return resourceHandler.createResourceListWithNamespace(cluster.getApiProvider(), namespace);
    }

    protected L createRawResourceListForAllNamespaces() throws ApiException {
        return resourceHandler.createResourceListWithoutNamespace(cluster.getApiProvider());
    }

    protected abstract int sortColumn(String sorted, SortDirection direction, T a, T b);

    @Getter
    public static class ResourceItem<V extends KubernetesObject> {
        protected UiUtil.COLOR altColor;
        protected UiUtil.COLOR color;
        protected long colorTimeout;
        protected String name;
        protected String namespace;
        protected long created;
        protected V resource;

        void initResource(V resource, boolean newResource) {
            this.name = resource.getMetadata().getName();
            this.namespace = resource.getMetadata().getNamespace();
            this.resource = resource;
            if (newResource)
                setFlashColor(UiUtil.COLOR.GREEN);
        }

        public String getAge() {
            return K8sUtil.getAge(resource.getMetadata().getCreationTimestamp());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (o instanceof ResourceItem res)
                return Objects.equals(name, res.name) && Objects.equals(namespace, res.namespace);
            if (o instanceof KubernetesObject res)
                return Objects.equals(name, res.getMetadata().getName()) && Objects.equals(namespace, res.getMetadata().getNamespace());
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name,namespace);
        }

        public void updateResource() {
            this.created = tryThis(() -> resource.getMetadata().getCreationTimestamp().toEpochSecond()).or(0L);
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
