package de.mhus.kt2l;

import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Pair;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1SecretList;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;

import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class GeneralGrid extends Grid<GeneralGrid.Resource> implements ResourcesGrid {

    private List<Resource> resourcesList = null;
    private List<Resource> filteredList = null;
    private String filterText = "";
    private String namespace;
    private CoreV1Api coreApi;
    private ClusterConfiguration.Cluster clusterConfig;
    private String resourceType;

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void refresh() {
        resourcesList = null;
        getDataProvider().refreshAll();
        UI.getCurrent().push();
    }

    @Override
    public void init(CoreV1Api coreApi, ClusterConfiguration.Cluster clusterConfig, ResourcesView view) {
        this.coreApi = coreApi;
        this.clusterConfig = clusterConfig;
        addClassNames("contact-grid");
        setSizeFull();
        addColumn(pod -> pod.name()).setHeader("Name").setSortProperty("name");
        addColumn(pod -> pod.age()).setHeader("Age").setSortProperty("age");
        getColumns().forEach(col -> col.setAutoWidth(true));
        setDataProvider(new ResourcesProvider());
    }

    @Override
    public void setFilter(String value) {
        filterText = value;
        if (resourcesList != null)
            getDataProvider().refreshAll();
    }

    @Override
    public void setNamespace(String value) {
        namespace = value;
        if (resourcesList != null) {
            resourcesList = null;
            getDataProvider().refreshAll();
        }
    }

    @Override
    public void setResourceType(String resourceType) {
        LOGGER.info("XXX Set resource type {}",resourceType);
        if (resourceType == null) return;
        this.resourceType = resourceType;
    }

    @Override
    public void handleShortcut(ShortcutEvent event) {

    }

    @Override
    public void setSelected() {

    }

    @Override
    public void destroy() {

    }

    private class ResourcesProvider extends CallbackDataProvider<Resource, Void> {

        public ResourcesProvider() {
            super(query -> {
                        LOGGER.info("Do the query {}",query);
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
                        LOGGER.info("Do the size query {}",query);
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
                                final var type = new TypeToken<Map<String, Object>>() {
                                }.getType();
                                final var items = (List<Map<String, Object>>) ((LinkedTreeMap<String,Object>)list).get("items");
                                items.forEach(item -> {
                                    final var metadata = (Map<String, Object>)((Map<String, Object>) item).get("metadata");
                                    final var name = (String) metadata.get("name");
                                    final var creationTimestamp = (String) metadata.get("creationTimestamp");
                                    resourcesList.add(new Resource(name, getAge(OffsetDateTime.parse(creationTimestamp)), OffsetDateTime.parse(creationTimestamp).toEpochSecond()));
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

    private String getAge(OffsetDateTime creationTimestamp) {
        final var age = System.currentTimeMillis()/1000 - creationTimestamp.toEpochSecond();
        if (age < 60) return age + "s";
        if (age < 3600) return age/60 + "m";
        if (age < 86400) return age/3600 + "h";
        return age/86400 + "d";
    }

    private void filterList() {
        if (resourcesList == null) {
            filteredList = Collections.emptyList();
        } else {
            final var filter = filterText;
            if (filter.isBlank()) {
                filteredList = resourcesList;
            } if (filter.startsWith("/")) {
                var f = filter.substring(1);
                filteredList = resourcesList.stream().filter(pod -> pod.name().matches(f)).collect(Collectors.toList());
            } else {
                filteredList = resourcesList.stream().filter(pod -> pod.name().contains(filter)).collect(Collectors.toList());
            }
        }
    }

    public record Resource(String name, String age, long created) {

    }

}


