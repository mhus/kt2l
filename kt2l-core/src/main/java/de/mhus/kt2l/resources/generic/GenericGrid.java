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

package de.mhus.kt2l.resources.generic;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import de.mhus.commons.tools.MObject;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.util.AbstractGrid;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Stream;

@Slf4j
public class GenericGrid extends AbstractGrid<GenericGrid.Resource, Component> {

    @Override
    protected void init() {

    }

    @Override
    public K8s getManagedResourceType() {
        return K8s.GENERIC;
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
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void onGridCellFocusChanged(Resource resource) {

    }

    @Override
    protected DataProvider<Resource, ?> createDataProvider() {
        return new ResourcesProvider();
    }

    @Override
    protected void createGridColumns(Grid<Resource> podGrid) {
        resourcesGrid.addColumn(pod -> pod.getName()).setHeader("Name").setSortProperty("name");
        resourcesGrid.addColumn(pod -> pod.getData()).setHeader("Data");
        resourcesGrid.addColumn(pod -> pod.getAge()).setHeader("Age").setSortProperty("age");
    }

    @Override
    protected boolean filterByContent(Resource pod, String filter) {
        return pod.getName() == null || pod.getName().contains(filter);
    }

    @Override
    protected boolean filterByRegex(Resource pod, String filter) {
        return pod.getName() == null || pod.getName().matches(filter);
    }

    @Override
    protected KubernetesObject getSelectedKubernetesObject(Resource resource) {
        return resource.getResource();
    }

    private V1APIResource resourceType;

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void refresh(long counter) {
        if (counter % 10 != 0) return;
        resourcesList = null;
        resourcesGrid.getDataProvider().refreshAll();
        getPanel().getCore().ui().push();
    }

    public void setResourceType(V1APIResource resourceType) {
        LOGGER.debug("Set resource type {}",resourceType);
        if (resourceType == null) return;
        this.resourceType = resourceType;
        super.setResourceType(K8s.CUSTOM);
    }

    @Override
    public void setResourceType(K8s resourceType) {
        LOGGER.debug("Set resource type {}",resourceType);
        this.resourceType = K8s.toResource(resourceType);
        super.setResourceType(K8s.CUSTOM);
    }

    private class ResourcesProvider extends CallbackDataProvider<Resource, Void> {

        public ResourcesProvider() {
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
                                case "age" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> MObject.compareTo(a.getCreated(), b.getCreated());
                                    case DESCENDING -> MObject.compareTo(b.getCreated(), a.getCreated());
                                };
                                default -> 0;
                            });

                        }
                        return filteredList.stream().skip(query.getOffset()).limit(query.getLimit());
                    }, query -> {
                        LOGGER.debug("Do the size query {}",query);
                        if (resourcesList == null) {
                            resourcesList = new ArrayList<>();
                            final var namespaceName = namespace ==  null || namespace.equals("all") ? null : (String) namespace;
                            final var genericApi = new GenericObjectsApi(cluster.getApiProvider().getClient(), resourceType);

                            try {

                                final var list = genericApi.listNamespacedCustomObject(namespaceName);

                                list.getItems().forEach(item -> {
//                                    final var metadata = (Map<String, Object>)((Map<String, Object>) item).get("metadata");
//                                    final var name = (String) metadata.get("name");
//                                    final var creationTimestamp = (String) metadata.get("creationTimestamp");
                                    final var name = item.getMetadata().getName();
                                    final var creationTimestamp = item.getMetadata().getCreationTimestamp();
                                    resourcesList.add(new Resource(
                                            name,
                                            item.toJson().replace("\n", ""),
//                                            getAge(OffsetDateTime.parse(creationTimestamp)),
                                            creationTimestamp,
                                            item
                                            )
                                    );
                                });

                            } catch (Exception e) {
                                LOGGER.error("Can't fetch resource from cluster",e);
                            }
                        }
                        filterList();
                        return filteredList.size();
                    }
            );
        }

    }

    @Getter
    public class Resource {
        private final String name;
        private final String data;
        private final OffsetDateTime created;
        private final KubernetesObject resource;

        Resource(String name, String data, OffsetDateTime created, KubernetesObject resource) {
            this.name = name;
            this.data = data;
            this.created = created;
            this.resource = resource;
        }

        public String getAge() {
            return K8sUtil.getAge(created);
        }
    }

}


