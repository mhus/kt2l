package de.mhus.kt2l.resources.util;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.commons.lang.IRegistration;
import de.mhus.commons.tools.MLang;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sService;
import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.Watch;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public abstract class AbstractGridWithoutNamespace<T extends AbstractGridWithoutNamespace.ResourceItem<V>, S extends Component,V extends KubernetesObject, L extends KubernetesListObject> extends AbstractGrid<T, S>{

    private IRegistration eventRegistration;
    @Autowired
    protected K8sService k8sService;
    protected HandlerK8s resourceHandler;

    @Override
    protected void init() {
        this.eventRegistration = ClusterBackgroundJob.instance(
                panel.getCore(),
                panel.getCluster(),
                getManagedWatchClass()
        ).getEventHandler().registerWeak(this::changeEvent);
        this.resourceHandler = k8sService.getResourceHandler(getManagedResourceType());
    }

    protected abstract Class<? extends ClusterBackgroundJob> getManagedWatchClass();

    private void changeEvent(Watch.Response<KubernetesObject> event) {
        if (resourcesList == null) return;

        if (event.type.equals(K8sUtil.WATCH_EVENT_ADDED) || event.type.equals(K8sUtil.WATCH_EVENT_MODIFIED)) {

            AtomicBoolean added = new AtomicBoolean(false);
            final var foundRes = MLang.synchronize(() -> resourcesList.stream().filter(res -> res.getName().equals(event.object.getMetadata().getName())).findFirst().orElseGet(
                    () -> {
                        final var res = createResourceItem((V)event.object);
                        resourcesList.add((T)res);
                        added.set(true);
                        return (T)res;
                    }
            ));

            foundRes.setResource((V)event.object);
            filterList();
            if (added.get())
                getPanel().getCore().ui().access(() -> resourcesGrid.getDataProvider().refreshAll());
            else
                getPanel().getCore().ui().access(() -> resourcesGrid.getDataProvider().refreshItem(foundRes));
        } else
        if (event.type.equals(K8sUtil.WATCH_EVENT_DELETED)) {
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

    protected abstract T createResourceItem(V object);

    @Override
    public abstract K8s getManagedResourceType();

    @Override
    protected void createDetailsComponent() {

    }

    @Override
    protected void onDetailsChanged(T item) {

    }

    @Override
    protected void onShowDetails(T item, boolean flip) {

    }

    @Override
    protected void onGridSelectionChanged() {

    }

    @Override
    protected abstract Class<T> getManagedResourceItemClass();

    @Override
    protected void onGridCellFocusChanged(T resource) {

    }

    @Override
    protected DataProvider<T, ?> createDataProvider() {
        return (DataProvider<T, ?>) new ResourceDataProvider();
    }

    @Override
    protected void createGridColumns(Grid<T> resourcesGrid) {
        resourcesGrid.addColumn(res -> res.getName()).setHeader("Name").setSortProperty("name");
        createGridColumnsAfterName(resourcesGrid);
        resourcesGrid.addColumn(res -> res.getAge()).setHeader("Age").setSortProperty("age");
    }

    protected abstract void createGridColumnsAfterName(Grid<T> resourcesGrid);

    @Override
    protected boolean filterByContent(T resource, String filter) {
        return resource.getName().contains(filter);
    }

    @Override
    protected boolean filterByRegex(T resource, String filter) {
        return resource.getName().matches(filter);
    }

    @Override
    protected KubernetesObject getSelectedKubernetesObject(T resource) {
        return resource.getResource();
    }

    @Override
    public void destroy() {
        if (eventRegistration != null)
            eventRegistration.unregister();
        super.destroy();
    }

    public class ResourceDataProvider extends CallbackDataProvider<ResourceItem<V>, Void> {
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
                                case "age" -> switch (queryOrder.getDirection()) {
                                    case ASCENDING -> Long.compare(a.getCreated(), b.getCreated());
                                    case DESCENDING -> Long.compare(b.getCreated(), a.getCreated());
                                };
                                default -> sortColumn(queryOrder.getSorted(), queryOrder.getDirection(), a, b);
                            });

                        }
                        return (Stream<ResourceItem<V>>) filteredList.stream().skip(query.getOffset()).limit(query.getLimit());
                    }, query -> {
                        LOGGER.debug("Do the size query {}",query);
                        if (resourcesList == null) {
                            resourcesList = new ArrayList<>();
                            synchronized (resourcesList) {
                                tryThis(() -> createResourceListWithoutNamespace())
                                        .onFailure(e -> LOGGER.error("Can't fetch resources from cluster", e))
                                        .onSuccess(list -> {
                                            list.getItems().forEach(res -> {
                                                resourcesList.add(createResourceItem((V) res));
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

    protected abstract int sortColumn(String sorted, SortDirection direction, T a, T b);

    protected L createResourceListWithoutNamespace() throws ApiException {
        return resourceHandler.createResourceListWithoutNamespace(cluster.getApiProvider());
    }

    @Data
    public static class ResourceItem<V extends KubernetesObject> {
        protected String name;
        protected long created;
        protected V resource;

        public ResourceItem(V resource) {
            this.name = resource.getMetadata().getName();
            this.resource = resource;
            this.created = tryThis(() -> resource.getMetadata().getCreationTimestamp().toEpochSecond()).or(0L);
        }

        public String getAge() {
            return K8sUtil.getAge(resource.getMetadata().getCreationTimestamp());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (o instanceof ResourceItem res)
                return Objects.equals(name, res.name);
            if (o instanceof KubernetesObject res)
                return Objects.equals(name, res.getMetadata().getName());
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

}
