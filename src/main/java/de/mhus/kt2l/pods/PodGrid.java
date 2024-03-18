package de.mhus.kt2l.pods;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import de.mhus.commons.lang.IRegistration;
import de.mhus.kt2l.generic.ActionService;
import de.mhus.kt2l.cluster.ClusterConfiguration;
import de.mhus.kt2l.cluster.ClusterPodWatch;
import de.mhus.kt2l.generic.ExecutionContext;
import de.mhus.kt2l.generic.IResourceProvider;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.generic.ResourceAction;
import de.mhus.kt2l.resources.ResourcesGrid;
import de.mhus.kt2l.resources.ResourcesGridPanel;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Watch;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.mhus.commons.tools.MCollection.cropArray;

@Slf4j
public class PodGrid extends AbstractGrid<PodGrid.Pod,Grid<PodGrid.Container>> {

    private List<Container> containerList = null;
    private Pod containerSelectedPod;
    private IRegistration podEventRegistration;

    protected void createDetailsComponent() {
        detailsComponent = new Grid<>(Container.class, false);
        addClassNames("contact-grid");
        detailsComponent.setWidthFull();
        detailsComponent.setHeight("200px");
        detailsComponent.addColumn(cont -> cont.name()).setHeader("Name").setSortProperty("name");
        detailsComponent.addColumn(cont -> cont.status()).setHeader("Status").setSortProperty("status");
        detailsComponent.addColumn(cont -> cont.age()).setHeader("Age").setSortProperty("age");
        detailsComponent.getColumns().forEach(col -> col.setAutoWidth(true));
        detailsComponent.setDataProvider(new ContainerProvider());
        detailsComponent.setVisible(false);
//        detailsComponent.addSelectionListener(event -> {
//            if (detailsComponent.isVisible())
//                actions.forEach(a -> a.updateWithContainer(event.getAllSelectedItems()));
//        });

//        GridContextMenu<Container> menu = detailsComponent.addContextMenu();
//        actions.forEach(action -> {
//            var item = menu.addItem(action.getAction().getTitle(), event ->
//                    action.execute()
//            );
//            action.setContainerContextMenuItem(item);
//        });

    }

    @Override
    protected void onDetailsChanged(Pod item) {
        setContainerPod(item);
    }

    @Override
    protected void onShowDetails(Pod item, boolean flip) {
        flipContainerVisibility(item, flip, !flip);
    }

    private void flipContainerVisibility(Pod pod, boolean flip, boolean focus) {
        containerList = null;
        containerSelectedPod = null;
        detailsComponent.setVisible(!flip || !detailsComponent.isVisible());
        if (detailsComponent.isVisible()) {
            containerSelectedPod = pod;
            detailsComponent.getDataProvider().refreshAll();
            if (focus)
                detailsComponent.getElement().getNode()
                        .runWhenAttached(ui -> ui.getPage().executeJs(
                                "setTimeout(function(){let firstTd = $0.shadowRoot.querySelector('tr:first-child > td:first-child'); firstTd.click(); firstTd.focus(); },0)", detailsComponent.getElement()));
        }

    }


    @Override
    protected void onGridSelectionChanged() {
        if (detailsComponent.isVisible())
            detailsComponent.deselectAll();
    }

    @Override
    protected Class<Pod> getManagedClass() {
        return Pod.class;
    }

    protected void onGridCellFocusChanged(PodGrid.Pod item) {
        if (detailsComponent.isVisible()) {
            containerSelectedPod = item;
            containerList = null;
            detailsComponent.getDataProvider().refreshAll();
        }
    }

    @Override
    protected DataProvider createDataProvider() {
        return new PodProvider();
    }

    @Override
    protected boolean filterByRegex(Pod pod, String f) {
        return pod.getName().matches(f);
    }

    @Override
    public void destroy() {
        podEventRegistration.unregister();
    }

    @Override
    protected void createGridColumns(Grid<Pod> podGrid) {
        podGrid.addColumn(pod -> pod.getName()).setHeader("Name").setSortProperty("name");
        podGrid.addColumn(pod -> pod.getStatus()).setHeader("Status").setSortProperty("status");
        podGrid.addColumn(pod -> pod.getAge()).setHeader("Age").setSortProperty("age");
    }

    @Override
    protected boolean filterByContent(Pod pod, String filter) {
        return pod.getName().contains(filter);
    }

    private void podEvent(Watch.Response<V1Pod> event) {
        if (podList == null) return;
        if (namespace != null && !namespace.equals(K8sUtil.NAMESPACE_ALL) && !namespace.equals(event.object.getMetadata().getNamespace())) return;

        if (event.type.equals(K8sUtil.WATCH_EVENT_ADDED) || event.type.equals(K8sUtil.WATCH_EVENT_MODIFIED)) {

            final var foundPod = podList.stream().filter(pod -> pod.getName().equals(event.object.getMetadata().getName())).findFirst().orElseGet(
                    () -> {
                        final var pod = new Pod(
                                event.object.getMetadata().getName(),
                                event.object.getMetadata().getNamespace(),
                                event.object.getStatus().getPhase(),
                                event.object.getMetadata().getCreationTimestamp().toEpochSecond(),
                                event.object
                        );
                        podList.add(pod);
                        return pod;
                    }
            );

            foundPod.setStatus(event.object.getStatus().getPhase());
            foundPod.setPod(event.object);
            filterList();
            podGrid.getDataProvider().refreshItem(foundPod);
        }

        if (event.type.equals(K8sUtil.WATCH_EVENT_DELETED)) {
            podList.forEach(pod -> {
                if (pod.getName().equals(event.object.getMetadata().getName())) {
                    podList.remove(pod);
                    filterList();
                    podGrid.getDataProvider().refreshAll();
                }
            });
        }

    }

    private static String getAge(OffsetDateTime creationTimestamp) {
        final var age = System.currentTimeMillis()/1000 - creationTimestamp.toEpochSecond();
        if (age < 60) return age + "s";
        if (age < 3600) return age/60 + "m";
        if (age < 86400) return age/3600 + "h";
        return age/86400 + "d";
    }

    @Override
    protected void init() {
        podEventRegistration = view.getMainView().getBackgroundJob(clusterConfig.name(), ClusterPodWatch.class, () -> new ClusterPodWatch()).getPodEventHandler().registerWeak(this::podEvent);
    }

    @Override
    public String getManagedResourceType() {
        return K8sUtil.RESOURCE_PODS;
    }

    private class ContainerProvider extends CallbackDataProvider<Container, Void> {
        public ContainerProvider() {
            super(query -> {
                        LOGGER.debug("Cont: Do the query {}",query);
                        for(QuerySortOrder queryOrder :
                                query.getSortOrders()) {
                            Collections.sort(containerList, (a, b) -> switch (queryOrder.getSorted()) {
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
                        return containerList.stream().skip(query.getOffset()).limit(query.getLimit());
                    }, query -> {
                        LOGGER.debug("Do the size query {}",query);
                        if (containerList == null) {
                            containerList = new ArrayList<>();

                            final var selectedPod = containerSelectedPod;
                            if (selectedPod == null) return 0;
                            try {
                                selectedPod.getPod().getStatus().getContainerStatuses().forEach(
                                        cs -> {
                                            containerList.add(new Container(
                                                    cs.getName(),
                                                    selectedPod.getNamespace(),
                                                    cs.getState().getWaiting() != null ? "Waiting" : "Running",
                                                    getAge(containerSelectedPod.getPod().getMetadata().getCreationTimestamp()), //TODO use container start time
                                                    selectedPod.getPod().getMetadata().getCreationTimestamp().toEpochSecond(),
                                                    selectedPod.getPod()
                                            ));
                                        }
                                );
                            } catch (Exception e) {
                                LOGGER.error("Can't fetch containers",e);
                            }
                        }
                        return containerList.size();
                    }
            );
        }
    }

    private class PodProvider extends CallbackDataProvider<Pod, Void> {

        public PodProvider() {
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
                        if (podList == null) {
                            podList = new ArrayList<>();
                            final var namespaceName = namespace ==  null || namespace.equals(K8sUtil.NAMESPACE_ALL) ? null : (String) namespace;
                            Try.of(() -> namespaceName == null ? coreApi.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null ) :  coreApi.listNamespacedPod(namespaceName, null, null, null, null, null, null, null, null, null, null))
                                    .onFailure(e -> LOGGER.error("Can't fetch pods from cluster",e))
                                    .onSuccess(podList -> {
                                        podList.getItems().forEach(pod -> {
                                            PodGrid.this.podList.add(new Pod(
                                                    pod.getMetadata().getName(),
                                                    pod.getMetadata().getNamespace(),
                                                    pod.getStatus().getPhase(),
                                                    pod.getMetadata().getCreationTimestamp().toEpochSecond(),
                                                    pod
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
    public static class Pod implements IResourceProvider {
        String name;
        String namespace;
        String status;
        long created;
        V1Pod pod;

        public String getAge() {
            return PodGrid.getAge(pod.getMetadata().getCreationTimestamp());
        }

        @Override
        public Object getResource() {
            return pod;
        }
    }

    public record Container(
            String name,
            String namespace,
            String status,
            String age,
            long created,
            V1Pod pod
    ) implements IResourceProvider {

        @Override
        public Object getResource() {
            return pod;
        }
    }

    private void setContainerPod(Pod item) {
        onGridCellFocusChanged(item);
    }

}


