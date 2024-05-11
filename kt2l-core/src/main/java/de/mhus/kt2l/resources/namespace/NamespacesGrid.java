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

package de.mhus.kt2l.resources.namespace;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import de.mhus.commons.lang.IRegistration;
import de.mhus.commons.tools.MLang;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGrid;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.util.Watch;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class NamespacesGrid extends AbstractGrid<NamespacesGrid.Namespace, Component> {

    private IRegistration namespaceEventRegistration;

    @Override
    protected void init() {
        namespaceEventRegistration = NamespaceWatch.instance(panel.getCore(), cluster, NamespaceWatch.class).getEventHandler().registerWeak(this::namespaceEvent);
    }

    private void namespaceEvent(Watch.Response<V1Namespace> event) {
        if (resourcesList == null) return;

        if (event.type.equals(K8s.WATCH_EVENT_ADDED) || event.type.equals(K8s.WATCH_EVENT_MODIFIED)) {

            final var foundRes = MLang.synchronize(() -> resourcesList.stream().filter(res -> res.getName().equals(event.object.getMetadata().getName())).findFirst().orElseGet(
                    () -> {
                        final var pod = new NamespacesGrid.Namespace(
                                event.object.getMetadata().getName(),
                                event.object
                        );
                        resourcesList.add(pod);
                        return pod;
                    }
            ), resourcesList);

            foundRes.setResource(event.object);
            filterList();
            getPanel().getCore().ui().access(() -> resourcesGrid.getDataProvider().refreshAll());
        } else
        if (event.type.equals(K8s.WATCH_EVENT_DELETED)) {
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

    @Override
    public K8s.RESOURCE getManagedResourceType() {
        return K8s.RESOURCE.NAMESPACE;
    }

    @Override
    protected void createDetailsComponent() {

    }

    @Override
    protected void onGridSelectionChanged() {

    }

    @Override
    protected Class<Namespace> getManagedResourceItemClass() {
        return Namespace.class;
    }

    @Override
    protected void onGridCellFocusChanged(Namespace namespace) {

    }

    @Override
    protected void onShowDetails(Namespace item, boolean flip) {

    }

    @Override
    protected void onDetailsChanged(Namespace item) {

    }

    @Override
    protected DataProvider<Namespace, ?> createDataProvider() {
        return new NamespacesDataProvider();
    }

    @Override
    protected KubernetesObject getSelectedKubernetesObject(Namespace resource) {
        return resource.getResource();
    }

    @Override
    protected boolean filterByRegex(Namespace resource, String filter) {
        return resource.name.matches(filter);
    }

    @Override
    protected boolean filterByContent(Namespace resource, String filter) {
        return resource.getName().contains(filter);
    }

    @Override
    protected void createGridColumns(Grid<Namespace> resourcesGrid) {
        resourcesGrid.addColumn(res -> res.getName()).setHeader("Name").setSortProperty("name");
    }

    @Override
    public void destroy() {
        if (namespaceEventRegistration != null)
            namespaceEventRegistration.unregister();
        super.destroy();
    }

    @Getter
    public class Namespace {

        private final String name;
        @Setter
        private V1Namespace resource;

        Namespace(String name, V1Namespace resource) {
            this.name = name;
            this.resource = resource;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (o instanceof NamespacesGrid.Namespace other)
                return Objects.equals(name, other.name);
            if (o instanceof V1Namespace other)
                return Objects.equals(name, other.getMetadata().getName());
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    private class NamespacesDataProvider extends CallbackDataProvider<NamespacesGrid.Namespace, Void> {
        public NamespacesDataProvider() {
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
                                default -> 0;
                            });

                        }
                        return filteredList.stream().skip(query.getOffset()).limit(query.getLimit());
                    }, query -> {
                        LOGGER.debug("Do the size query {}",query);
                        if (resourcesList == null) {
                            resourcesList = new ArrayList<>();
                            synchronized (resourcesList) {
                                tryThis(() -> cluster.getApiProvider().getCoreV1Api().listNamespace().execute())
                                        .onFailure(e -> LOGGER.error("Can't fetch pods from cluster", e))
                                        .onSuccess(list -> {
                                            list.getItems().forEach(res -> {
                                                NamespacesGrid.this.resourcesList.add(new NamespacesGrid.Namespace(
                                                        res.getMetadata().getName(),
                                                        res
                                                ));
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
}
