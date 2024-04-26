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

package de.mhus.kt2l.resources.nodes;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import de.mhus.commons.lang.IRegistration;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.AbstractGrid;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.util.Watch;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Stream;

@Slf4j
public class NodesGrid extends AbstractGrid<NodesGrid.Nodes, Component> {


    private IRegistration eventRegistration;

    @Override
    protected void init() {
        eventRegistration = ClusterNodeWatch.instance(view.getCore(), view.getClusterConfig()).getEventHandler().registerWeak(this::nodeEvent);
    }

    private void nodeEvent(Watch.Response<V1Node> event) {
        if (resourcesList == null) return;
        if (namespace != null && !namespace.equals(K8sUtil.NAMESPACE_ALL) && !namespace.equals(event.object.getMetadata().getNamespace())) return;

        if (event.type.equals(K8sUtil.WATCH_EVENT_ADDED) || event.type.equals(K8sUtil.WATCH_EVENT_MODIFIED)) {

            final var foundPod = resourcesList.stream().filter(pod -> pod.getName().equals(event.object.getMetadata().getName())).findFirst().orElseGet(
                    () -> {
                        final var pod = new NodesGrid.Nodes(
                                event.object.getMetadata().getName(),
                                event.object.getMetadata().getName(), //XXX
                                event.object.getMetadata().getCreationTimestamp().toEpochSecond(),
                                event.object
                        );
                        resourcesList.add(pod);
                        return pod;
                    }
            );

            foundPod.setStatus(event.object.getStatus().getPhase());
            foundPod.setNode(event.object);
            filterList();
            resourcesGrid.getDataProvider().refreshItem(foundPod);
        }

        if (event.type.equals(K8sUtil.WATCH_EVENT_DELETED)) {
            resourcesList.forEach(pod -> {
                if (pod.getName().equals(event.object.getMetadata().getName())) {
                    resourcesList.remove(pod);
                    filterList();
                    resourcesGrid.getDataProvider().refreshAll();
                }
            });
        }

    }

    @Override
    public String getManagedResourceType() {
        return K8sUtil.RESOURCE_NODES;
    }

    @Override
    protected void createDetailsComponent() {

    }

    @Override
    protected void onDetailsChanged(Nodes item) {

    }

    @Override
    protected void onShowDetails(Nodes item, boolean flip) {

    }

    @Override
    protected void onGridSelectionChanged() {

    }

    @Override
    protected Class<Nodes> getManagedClass() {
        return Nodes.class;
    }

    @Override
    protected void onGridCellFocusChanged(Nodes nodes) {

    }

    @Override
    protected DataProvider<Nodes, ?> createDataProvider() {
        return new NodesDataProvider();
    }

    @Override
    protected void createGridColumns(Grid<Nodes> resourcesGrid) {
        resourcesGrid.addColumn(res -> res.getName()).setHeader("Name").setSortProperty("name");
        resourcesGrid.addColumn(res -> res.getStatus()).setHeader("Status").setSortProperty("status");
        resourcesGrid.addColumn(res -> res.getAge()).setHeader("Age").setSortProperty("age");
    }

    @Override
    protected boolean filterByContent(Nodes resource, String filter) {
        return resource.getName().contains(filter);
    }

    @Override
    protected boolean filterByRegex(Nodes resource, String filter) {
        return resource.getName().matches(filter);
    }

    @Override
    protected KubernetesObject getSelectedKubernetesObject(Nodes resource) {
        return resource.getNode();
    }

    @Override
    public void destroy() {
        eventRegistration.unregister();
    }

    public class NodesDataProvider extends CallbackDataProvider<Nodes, Void> {
        public NodesDataProvider() {
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
                            Try.of(() -> coreApi.listNode(null, null, null, null, null, null, null, null, null, null ) )
                                    .onFailure(e -> LOGGER.error("Can't fetch pods from cluster",e))
                                    .onSuccess(list -> {
                                        list.getItems().forEach(res -> {
                                            NodesGrid.this.resourcesList.add(new Nodes(
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

    @Data
    @AllArgsConstructor
    public static class Nodes {
        String name;
        String status;
        long created;
        V1Node node;

        public String getAge() {
            return K8sUtil.getAge(node.getMetadata().getCreationTimestamp());
        }

    }
}
