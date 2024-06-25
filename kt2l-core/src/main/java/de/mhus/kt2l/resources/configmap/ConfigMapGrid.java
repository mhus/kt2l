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

package de.mhus.kt2l.resources.configmap;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGridWithNamespace;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class ConfigMapGrid extends AbstractGridWithNamespace<ConfigMapGrid.Resource, Component, V1ConfigMap, V1ConfigMapList> {

    @Autowired
    private PanelService panelService;

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return ConfigMapWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(Resource::getDataCnt).setHeader("Data Cnt").setSortProperty("datacnt");
        resourcesGrid.addColumn(v -> MString.toByteDisplayString(v.getDataSize()) ).setHeader("Data Size").setSortProperty("datasize");
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        return switch (sorted) {
            case "datacnt" -> switch (direction) {
                case ASCENDING -> Integer.compare(a.getDataCnt(), b.getDataCnt());
                case DESCENDING -> Integer.compare(b.getDataCnt(), a.getDataCnt());
            };
            case "datasize" -> switch (direction) {
                case ASCENDING -> Long.compare(a.getDataSize(), b.getDataSize());
                case DESCENDING -> Long.compare(b.getDataSize(), a.getDataSize());
            };
            default -> 0;
        };
    }

    @Override
    protected Resource createResourceItem() {
        return new Resource();
    }

    @Override
    public V1APIResource getManagedType() {
        return K8s.CONFIG_MAP;
    }

    @Override
    protected void onShowDetails(Resource item, boolean flip) {
        panelService.showEditConfigMapPanel(panel.getTab(), panel.getCore(), cluster, item.getResource()).select();
    }

    @Getter
    public static class Resource extends ResourceItem<V1ConfigMap> {
        private int dataCnt;
        private long dataSize;

        @Override
        public void updateResource() {
            super.updateResource();
            dataCnt = tryThis(() -> resource.getData().size()).or(0);
            dataSize = tryThis(() -> resource.getData().values().stream().mapToLong(String::length).sum()).or(0L);
            setColor(null);
        }

        private String toStringOr0(Integer integer) {
            if (integer == null) return "0";
            return integer.toString();
        }
    }
}
