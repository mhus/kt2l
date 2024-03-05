package de.mhus.kt2l;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class PodGrid extends Grid<PodGrid.Pod> implements ResourcesGrid {

    private List<Pod> podList = null;
    private List<Pod> filteredList = null;
    private String filterText = "";
    private String namespace;
    private CoreV1Api coreApi;
    private ClusterConfiguration.Cluster clusterConfig;

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void refresh() {
        podList = null;
        getDataProvider().refreshAll();
        UI.getCurrent().push();
    }

    @Override
    public void init(CoreV1Api coreApi, ClusterConfiguration.Cluster clusterConfig) {
        this.coreApi = coreApi;
        this.clusterConfig = clusterConfig;
        addClassNames("contact-grid");
        setSizeFull();
        addColumn(pod -> pod.name()).setHeader("Name").setSortProperty("name");
        addColumn(pod -> pod.status()).setHeader("Status").setSortProperty("status");
        addColumn(pod -> pod.age()).setHeader("Age").setSortProperty("age");
        getColumns().forEach(col -> col.setAutoWidth(true));
        setDataProvider(new PodProvider());
    }

    @Override
    public void setFilter(String value) {
        filterText = value;
        if (podList != null)
            getDataProvider().refreshAll();
    }

    @Override
    public void setNamespace(String value) {
        namespace = value;
        if (podList != null) {
            podList = null;
            getDataProvider().refreshAll();
        }
    }

    @Override
    public void setResourceType(String resourceType) {

    }

    private class PodProvider extends CallbackDataProvider<Pod, Void> {

        public PodProvider() {
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
                                case "status" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> a.status().compareTo(b.status());
                                    case DESCENDING -> b.status().compareTo(a.status());
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
                        if (podList == null) {
                            podList = new ArrayList<>();
                            final var namespaceName = namespace ==  null || namespace.equals(K8sUtil.NAMESPACE_ALL) ? null : (String) namespace;
                            Try.of(() -> namespaceName == null ? coreApi.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null ) :  coreApi.listNamespacedPod(namespaceName, null, null, null, null, null, null, null, null, null, null))
                                    .onFailure(e -> LOGGER.error("Can't fetch pods from cluster",e))
                                    .onSuccess(podList -> {
                                        podList.getItems().forEach(pod -> {
                                            PodGrid.this.podList.add(new Pod(pod.getMetadata().getName(), pod.getStatus().getPhase(), getAge(pod.getMetadata().getCreationTimestamp()), pod.getMetadata().getCreationTimestamp().toEpochSecond()));
                                        });
                                    });
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
        if (podList == null) {
            filteredList = Collections.emptyList();
        } else {
            final var filter = filterText;
            if (filter.isBlank()) {
                filteredList = podList;
            } if (filter.startsWith("/")) {
                var f = filter.substring(1);
                filteredList = podList.stream().filter(pod -> pod.name().matches(f)).collect(Collectors.toList());
            } else {
                filteredList = podList.stream().filter(pod -> pod.name().contains(filter)).collect(Collectors.toList());
            }
        }
    }

    public record Pod(String name, String status, String age, long created) {

    }

}


