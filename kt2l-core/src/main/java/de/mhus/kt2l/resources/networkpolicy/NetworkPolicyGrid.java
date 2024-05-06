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

package de.mhus.kt2l.resources.networkpolicy;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import de.mhus.commons.lang.IRegistration;
import de.mhus.commons.tools.MLang;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.AbstractGrid;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.V1NetworkPolicy;
import io.kubernetes.client.openapi.models.V1NetworkPolicyList;
import io.kubernetes.client.util.Watch;
import io.vavr.control.Try;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Slf4j
public class NetworkPolicyGrid extends AbstractGrid<NetworkPolicyGrid.Resource, Component> {

    private IRegistration eventRegistration;
    private NetworkingV1Api networkingV1Api;

    @Override
    protected void init() {
        networkingV1Api = new NetworkingV1Api(coreApi.getApiClient());
        eventRegistration = NetworkPolicyWatch.instance(view.getCore(), view.getCluster()).getEventHandler().registerWeak(this::changeEvent);
    }

    private void changeEvent(Watch.Response<V1NetworkPolicy> event) {
        if (resourcesList == null) return;
        if (namespace != null && !namespace.equals(K8s.NAMESPACE_ALL) && !namespace.equals(event.object.getMetadata().getNamespace())) return;

        if (event.type.equals(K8s.WATCH_EVENT_ADDED) || event.type.equals(K8s.WATCH_EVENT_MODIFIED)) {

            AtomicBoolean added = new AtomicBoolean(false);
            final var foundRes = resourcesList.stream().filter(res -> res.getName().equals(event.object.getMetadata().getName())).findFirst().orElseGet(
                    () -> {
                        final var res = new Resource(event.object);
                        resourcesList.add(res);
                        added.set(true);
                        return res;
                    }
            );

            foundRes.setResource(event.object);
            filterList();
            if (added.get())
                getView().getCore().ui().access(() -> resourcesGrid.getDataProvider().refreshAll());
            else
                getView().getCore().ui().access(() -> resourcesGrid.getDataProvider().refreshItem(foundRes));
        } else
        if (event.type.equals(K8s.WATCH_EVENT_DELETED)) {
            resourcesList.forEach(res -> {
                if (res.getName().equals(event.object.getMetadata().getName())) {
                    resourcesList.remove(res);
                    filterList();
                    getView().getCore().ui().access(() -> resourcesGrid.getDataProvider().refreshAll());
                }
            });
        }

    }

    @Override
    public K8s.RESOURCE getManagedResourceType() {
        return K8s.RESOURCE.NETWORK_POLICY;
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
    protected void onGridCellFocusChanged(Resource resources) {

    }

    @Override
    protected DataProvider<Resource, ?> createDataProvider() {
        return new ResourceDataProvider();
    }

    @Override
    protected void createGridColumns(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(res -> res.getName()).setHeader("Name").setSortProperty("name");
        resourcesGrid.addColumn(res -> res.getNamespace()).setHeader("Namespace").setSortProperty("namespace");
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
                                case "namespace" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> a.getNamespace().compareTo(b.getNamespace());
                                    case DESCENDING -> b.getNamespace().compareTo(a.getNamespace());
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
                                            NetworkPolicyGrid.this.resourcesList.add(new Resource(res));
                                        });
                                    });
                        }
                        filterList();
                        return filteredList.size();
                    }
            );
        }

    }

    private V1NetworkPolicyList createRawResourceList() throws ApiException {
        if (namespace == null || namespace.equals(K8s.NAMESPACE_ALL))
            return networkingV1Api.listNetworkPolicyForAllNamespaces().execute();
        return networkingV1Api.listNamespacedNetworkPolicy(namespace).execute();
    }

    @Data
    public static class Resource {
        String name;
        String namespace;
        long created;
        V1NetworkPolicy resource;

        public Resource(V1NetworkPolicy resource) {
            this.name = resource.getMetadata().getName();
            this.namespace = resource.getMetadata().getNamespace();
            this.created = MLang.tryThis(() -> resource.getMetadata().getCreationTimestamp().toEpochSecond()).or(0L);
            this.resource = resource;
        }

        public String getAge() {
            return K8s.getAge(resource.getMetadata().getCreationTimestamp());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (o instanceof Resource res)
                return Objects.equals(name, res.name) && Objects.equals(namespace, res.namespace);
            if (o instanceof V1NetworkPolicy res)
                return Objects.equals(name, res.getMetadata().getName()) && Objects.equals(namespace, res.getMetadata().getNamespace());
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, namespace);
        }
    }
}
