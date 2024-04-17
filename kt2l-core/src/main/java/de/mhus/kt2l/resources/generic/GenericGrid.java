/**
 * This file is part of kt2l-core.
 *
 * kt2l-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * kt2l-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with kt2l-core.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l.resources.generic;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import de.mhus.kt2l.k8s.GenericObjectsApi;
import de.mhus.kt2l.resources.AbstractGrid;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
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
    public String getManagedResourceType() {
        return "";
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
        return new ResourcesProvider();
    }

    @Override
    protected void createGridColumns(Grid<Resource> podGrid) {
        podGrid.addColumn(pod -> pod.name()).setHeader("Name").setSortProperty("name");
        podGrid.addColumn(pod -> pod.age()).setHeader("Age").setSortProperty("age");
    }

    @Override
    protected boolean filterByContent(Resource pod, String filter) {
        return pod.name() == null || pod.name().contains(filter);
    }

    @Override
    protected boolean filterByRegex(Resource pod, String filter) {
        return pod.name() == null || pod.name().matches(filter);
    }

    @Override
    protected KubernetesObject getSelectedKubernetesObject(Resource resource) {
        return resource.resource();
    }

    @Override
    public void destroy() {

    }

//    private List<Resource> resourcesList = null;
//    private List<Resource> filteredList = null;
//    private String filterText = "";
//    private String namespace;
//    private CoreV1Api coreApi;
//    private ClusterConfiguration.Cluster clusterConfig;
    private String resourceType;
//
//    @Override
//    public Component getComponent() {
//        return this;
//    }
//
    @Override
    public void refresh(long counter) {
        if (counter % 10 != 0) return;
        resourcesList = null;
        resourcesGrid.getDataProvider().refreshAll();
        UI.getCurrent().push();
    }
//
//    @Override
//    public void init(CoreV1Api coreApi, ClusterConfiguration.Cluster clusterConfig, ResourcesGridPanel view) {
//        this.coreApi = coreApi;
//        this.clusterConfig = clusterConfig;
//        addClassNames("contact-grid");
//        setSizeFull();
//        addColumn(pod -> pod.name()).setHeader("Name").setSortProperty("name");
//        addColumn(pod -> pod.age()).setHeader("Age").setSortProperty("age");
//        getColumns().forEach(col -> col.setAutoWidth(true));
//        setDataProvider(new ResourcesProvider());
//    }
//
//    @Override
//    public void setFilter(String value) {
//        filterText = value;
//        if (resourcesList != null)
//            getDataProvider().refreshAll();
//    }
//
//    @Override
//    public void setNamespace(String value) {
//        namespace = value;
//        if (resourcesList != null) {
//            resourcesList = null;
//            getDataProvider().refreshAll();
//        }
//    }
//
//    @Override
    public void setResourceType(String resourceType) {
        LOGGER.debug("Set resource type {}",resourceType);
        if (resourceType == null) return;
        this.resourceType = resourceType;
    }
//
//    @Override
//    public void handleShortcut(ShortcutEvent event) {
//
//    }
//
//    @Override
//    public void setSelected() {
//
//    }
//
//    @Override
//    public void setUnselected() {
//
//    }
//
//    @Override
//    public void destroy() {
//
//    }
//
    private class ResourcesProvider extends CallbackDataProvider<Resource, Void> {

        public ResourcesProvider() {
            super(query -> {
                        LOGGER.debug("Do the query {}",query);
                        if (filteredList == null) return Stream.empty();
                        for(QuerySortOrder queryOrder :
                                query.getSortOrders()) {
                            Collections.sort(filteredList, (a, b) -> switch (queryOrder.getSorted()) {
                                case "name" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> a.name().compareTo(b.name());
                                    case DESCENDING -> b.name().compareTo(a.name());
                                };
                                case "age" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> Long.compare(a.created(), b.created());
                                    case DESCENDING -> Long.compare(b.created(), a.created());
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
                            final var genericApi = new GenericObjectsApi(coreApi.getApiClient());

                            try {
                                // v1/pods
                                // apps/v1/daemonsets
                                // storage.k8s.io/v1/csidrivers
                                final var parts = resourceType.split("/");
                                String group = null;
                                String version = "v1";
                                String plural = null;
                                if (parts.length == 3) {
                                    group = parts[0];
                                    version = parts[1];
                                    plural = parts[2];
                                } else if (parts.length == 2) {
                                    group = parts[0];
                                    plural = parts[1];
                                } else {
                                    plural = parts[0];
                                }

                                final var list = genericApi.listNamespacedCustomObject(group, version, null, plural, null, null, null, null, null, null, null, null, null, null);

                                list.forEach(item -> {
//                                    final var metadata = (Map<String, Object>)((Map<String, Object>) item).get("metadata");
//                                    final var name = (String) metadata.get("name");
//                                    final var creationTimestamp = (String) metadata.get("creationTimestamp");
                                    final var name = item.getMetadata().getName();
                                    final var creationTimestamp = item.getMetadata().getCreationTimestamp();
                                    resourcesList.add(new Resource(
                                            name,
//                                            getAge(OffsetDateTime.parse(creationTimestamp)),
                                            getAge(creationTimestamp),
//                                            OffsetDateTime.parse(creationTimestamp).toEpochSecond(),
                                            creationTimestamp == null ? 0 : creationTimestamp.toEpochSecond(),
                                            item
                                            )
                                    );
                                });

                            } catch (ApiException e) {
                                LOGGER.error("Can't fetch pods from cluster",e);
                            }
                        }
                        filterList();
                        return filteredList.size();
                    }
            );
        }

    }
//
    private String getAge(OffsetDateTime creationTimestamp) {
        if (creationTimestamp == null) return "";
        final var age = System.currentTimeMillis()/1000 - creationTimestamp.toEpochSecond();
        if (age < 60) return age + "s";
        if (age < 3600) return age/60 + "m";
        if (age < 86400) return age/3600 + "h";
        return age/86400 + "d";
    }

    public record Resource(String name, String age, long created, KubernetesObject resource) {
    }

}


