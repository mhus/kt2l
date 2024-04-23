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

package de.mhus.kt2l.resources.namespaces;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.AbstractGrid;
import de.mhus.kt2l.resources.nodes.NodesGrid;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Stream;

@Slf4j
public class NamespacesGrid extends AbstractGrid<NamespacesGrid.Namespace, Component> {

    @Override
    protected void init() {

    }

    @Override
    public String getManagedResourceType() {
        return K8sUtil.RESOURCE_NAMESPACE;
    }

    @Override
    protected void createDetailsComponent() {

    }

    @Override
    protected void onGridSelectionChanged() {

    }

    @Override
    protected Class<Namespace> getManagedClass() {
        return Namespace.class;
    }

    @Override
    protected void onGridCellFocusChanged(Namespace namespace) {

    }

    @Override
    protected void onShowDetails(Namespace item, boolean flip) {

    }

    @Override
    protected void onDetailsChanged(Namespace item) {

    }

    @Override
    protected DataProvider<Namespace, ?> createDataProvider() {
        return new NamespacesDataProvider();
    }

    @Override
    protected KubernetesObject getSelectedKubernetesObject(Namespace resource) {
        return resource.resource();
    }

    @Override
    protected boolean filterByRegex(Namespace resource, String filter) {
        return resource.name.matches(filter);
    }

    @Override
    protected boolean filterByContent(Namespace resource, String filter) {
        return resource.name().contains(filter);
    }

    @Override
    protected void createGridColumns(Grid<Namespace> resourcesGrid) {
        resourcesGrid.addColumn(res -> res.name()).setHeader("Name").setSortProperty("name");
    }

    @Override
    public void destroy() {

    }

    public record Namespace(String name, V1Namespace resource) {

    }

    private class NamespacesDataProvider extends CallbackDataProvider<NamespacesGrid.Namespace, Void> {
        public NamespacesDataProvider() {
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
                                default -> 0;
                            });

                        }
                        return filteredList.stream().skip(query.getOffset()).limit(query.getLimit());
                    }, query -> {
                        LOGGER.debug("Do the size query {}",query);
                        if (resourcesList == null) {
                            resourcesList = new ArrayList<>();
                            Try.of(() -> coreApi.listNamespace(null, null, null, null, null, null, null, null, null, null ) )
                                    .onFailure(e -> LOGGER.error("Can't fetch pods from cluster",e))
                                    .onSuccess(list -> {
                                        list.getItems().forEach(res -> {
                                            NamespacesGrid.this.resourcesList.add(new NamespacesGrid.Namespace(
                                                    res.getMetadata().getName(),
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
}