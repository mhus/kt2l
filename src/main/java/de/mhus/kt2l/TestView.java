package de.mhus.kt2l;

import com.vaadin.flow.component.DetachNotifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.provider.SortOrder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.vavr.control.Try;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@PermitAll
@Route(value = "test/:clusterId*", layout = MainLayout.class)
public class TestView extends VerticalLayout implements BeforeEnterObserver, BeforeLeaveObserver {

    @Autowired
    private K8sService k8s;

    @Autowired
    ScheduledExecutorService scheduler;

    private String clusterId;
    private Grid<Pod> grid;
    private TextField filterText;
    private List<Pod> podList = null;
    private List<Pod> filteredList;
    private ComboBox<String> namespaceSelector;
    private CoreV1Api coreApi;
    private UI ui;
    private ScheduledFuture<?> closeScheduler;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        clusterId = event.getRouteParameters().get("clusterId").get();

        coreApi = Try.of(() -> k8s.getCoreV1Api(clusterId)).onFailure(e -> LOGGER.error("Error ",e) ).get();
        LOGGER.info("ClusterId: {}",clusterId);
        if (grid == null)
            createUI();

        if (event.getRedirectQueryParameters() != null) {
            final var searchString = event.getRedirectQueryParameters().getSingleParameter("search");
            final var sortString = event.getRedirectQueryParameters().getSingleParameter("sort");
            final var namespaceString = event.getRedirectQueryParameters().getSingleParameter("namespace");
            namespaceString.ifPresent(s -> namespaceSelector.setValue(s));
            searchString.ifPresent(s -> filterText.setValue(s));
            sortString.ifPresent(s -> {
                grid.setColumnOrder(grid.getColumnByKey(s));
            });
        }

        closeScheduler = scheduler.scheduleAtFixedRate(this::refresh, 10, 10, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        LOGGER.info("Leave");
        closeScheduler.cancel(false);
    }

    public void createUI() {
        removeAll();
        grid = new Grid<>(Pod.class);
        filterText = new TextField();
        namespaceSelector = new ComboBox<String>();

        addClassName("list-view");
        setSizeFull();
        configureGrid();

        add(getToolbar(), grid);
        this.ui = UI.getCurrent();

    }

//    @Scheduled(fixedRate = 10000)
    public void refresh() {
        if (ui != null && grid != null && grid.getDataProvider() != null) {
            ui.access(() -> {
                LOGGER.info("Refresh");
                podList = null;
                grid.getDataProvider().refreshAll();
                ui.push();
            });
        }
    }

    private void configureGrid() {
        grid.addClassNames("contact-grid");
        grid.setSizeFull();
        grid.addColumn(pod -> pod.name()).setHeader("Name").setSortProperty("name");
        grid.addColumn(pod -> pod.status()).setHeader("Status").setSortProperty("status");
        grid.addColumn(pod -> pod.age()).setHeader("Age").setSortProperty("age");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        grid.setDataProvider(new PodProvider());
    }

    private HorizontalLayout getToolbar() {
        
        namespaceSelector.setItems(K8sUtil.geNamespacesWithAll(coreApi));
        namespaceSelector.setPlaceholder("Namespace");
        namespaceSelector.addValueChangeListener(e -> {
            podList = null;
            grid.getDataProvider().refreshAll();
            UI.getCurrent().getPage().getHistory().pushState(null, "test/"+clusterId + "?namespace="+e.getValue());
        });

        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> {
            grid.getDataProvider().refreshAll();
        });


        var toolbar = new HorizontalLayout(filterText, namespaceSelector);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private record Pod(String name, String status, String age, long created) {

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
                            final var namespaceName = namespaceSelector.getValue() ==  null || namespaceSelector.getValue().equals("all") ? null : (String) namespaceSelector.getValue();
                            Try.of(() -> namespaceName == null ? coreApi.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null ) :  coreApi.listNamespacedPod(namespaceName, null, null, null, null, null, null, null, null, null, null))
                                    .onFailure(e -> LOGGER.error("Can't fetch pods from cluster",e))
                                    .onSuccess(podList -> {
                                        podList.getItems().forEach(pod -> {
                                            TestView.this.podList.add(new Pod(pod.getMetadata().getName(), pod.getStatus().getPhase(), getAge(pod.getMetadata().getCreationTimestamp()), pod.getMetadata().getCreationTimestamp().toEpochSecond()));
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
            final var filter = filterText.getValue();
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
}
