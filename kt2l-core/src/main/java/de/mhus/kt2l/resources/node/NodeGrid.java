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

package de.mhus.kt2l.resources.node;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import de.mhus.commons.lang.IRegistration;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.AbstractGrid;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.util.Watch;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Slf4j
public class NodeGrid extends AbstractGrid<NodeGrid.Resource, Component> {


    private IRegistration eventRegistration;

    @Override
    protected void init() {
        eventRegistration = NodeWatch.instance(view.getCore(), view.getClusterConfig()).getEventHandler().registerWeak(this::changeEvent);
    }

    private void changeEvent(Watch.Response<V1Node> event) {
        if (resourcesList == null) return;

        if (event.type.equals(K8s.WATCH_EVENT_ADDED) || event.type.equals(K8s.WATCH_EVENT_MODIFIED)) {

            AtomicBoolean added = new AtomicBoolean(false);
            final var foundRes = resourcesList.stream().filter(res -> res.getName().equals(event.object.getMetadata().getName())).findFirst().orElseGet(
                    () -> {
                        final var res = new Resource(
                                event.object.getMetadata().getName(),
                                "", //XXX
                                event.object.getMetadata().getCreationTimestamp().toEpochSecond(),
                                event.object
                        );
                        resourcesList.add(res);
                        added.set(true);
                        return res;
                    }
            );

            foundRes.setStatus(event.object.getStatus().getPhase());
            foundRes.setResource(event.object);
            filterList();
            if (added.get())
                getUI().get().access(() -> resourcesGrid.getDataProvider().refreshAll());
            else
                getUI().get().access(() -> resourcesGrid.getDataProvider().refreshItem(foundRes));
        } else
        if (event.type.equals(K8s.WATCH_EVENT_DELETED)) {
            resourcesList.forEach(res -> {
                if (res.getName().equals(event.object.getMetadata().getName())) {
                    resourcesList.remove(res);
                    filterList();
                    getUI().get().access(() -> resourcesGrid.getDataProvider().refreshAll());
                }
            });
        }

    }

    @Override
    public K8s.RESOURCE getManagedResourceType() {
        return K8s.RESOURCE.NODE;
    }

    @Override
    protected void createDetailsComponent() {

    }

    @Override
    protected void onDetailsChanged(Resource item) {

    }

    @Override
    protected void onShowDetails(Resource item, boolean flip) {

    }

    @Override
    protected void onGridSelectionChanged() {

    }

    @Override
    protected Class<Resource> getManagedClass() {
        return Resource.class;
    }

    @Override
    protected void onGridCellFocusChanged(Resource resource) {

    }

    @Override
    protected DataProvider<Resource, ?> createDataProvider() {
        return new ResourceDataProvider();
    }

    @Override
    protected void createGridColumns(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(res -> res.getName()).setHeader("Name").setSortProperty("name");
        resourcesGrid.addColumn(res -> res.getStatus()).setHeader("Status").setSortProperty("status");
        resourcesGrid.addColumn(res -> res.getAge()).setHeader("Age").setSortProperty("age");
    }

    @Override
    protected boolean filterByContent(Resource resource, String filter) {
        return resource.getName().contains(filter);
    }

    @Override
    protected boolean filterByRegex(Resource resource, String filter) {
        return resource.getName().matches(filter);
    }

    @Override
    protected KubernetesObject getSelectedKubernetesObject(Resource resource) {
        return resource.getResource();
    }

    @Override
    public void destroy() {
        if (eventRegistration != null)
            eventRegistration.unregister();
        super.destroy();
    }

    public class ResourceDataProvider extends CallbackDataProvider<Resource, Void> {
        public ResourceDataProvider() {
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
                        if (resourcesList == null) {
                            resourcesList = new ArrayList<>();
                            Try.of(() -> createRawResourceList() )
                                    .onFailure(e -> LOGGER.error("Can't fetch resources from cluster",e))
                                    .onSuccess(list -> {
                                        list.getItems().forEach(res -> {
                                            NodeGrid.this.resourcesList.add(new Resource(
                                                    res.getMetadata().getName(),
                                                    res.getMetadata().getName(), //XXX
                                                    res.getMetadata().getCreationTimestamp().toEpochSecond(),
                                                    res
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

    private V1NodeList createRawResourceList() throws ApiException {
        return coreApi.listNode().execute();
    }

    @Data
    @AllArgsConstructor
    public static class Resource {
        String name;
        String status;
        long created;
        V1Node resource;

        public String getAge() {
            return K8s.getAge(resource.getMetadata().getCreationTimestamp());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (o instanceof Resource res)
                return Objects.equals(name, res.name);
            if (o instanceof V1Node res)
                return Objects.equals(name, res.getMetadata().getName());
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
