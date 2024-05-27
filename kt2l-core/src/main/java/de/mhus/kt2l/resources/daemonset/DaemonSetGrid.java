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

package de.mhus.kt2l.resources.daemonset;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.UiUtil;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGridWithNamespace;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1DaemonSetList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DaemonSetGrid extends AbstractGridWithNamespace<DaemonSetGrid.Resource, Component, V1DaemonSet, V1DaemonSetList> {

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return DaemonSetWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(Resource::getStatus).setHeader("Status").setSortProperty("status").setSortable(true);
        resourcesGrid.addColumn(Resource::getDesired).setHeader("Desired").setSortable(false);
        resourcesGrid.addColumn(Resource::getCurrent).setHeader("Current").setSortable(false);
        resourcesGrid.addColumn(Resource::getReady).setHeader("Ready").setSortable(false);
        resourcesGrid.addColumn(Resource::getUpToDate).setHeader("Up To Date").setSortable(false);
        resourcesGrid.addColumn(Resource::getAvailable).setHeader("Available").setSortable(false);
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        if ("status".equals(sorted)) {
            switch (direction) {
                case ASCENDING: return a.getStatus().compareTo(b.getStatus());
                case DESCENDING: return b.getStatus().compareTo(a.getStatus());
            }
        }
        return 0;
    }

    @Override
    protected Resource createResourceItem() {
        return new Resource();
    }

    @Override
    public K8s getManagedResourceType() {
        return K8s.DAEMON_SET;
    }

    @Getter
    public static class Resource extends ResourceItem<V1DaemonSet> {
        String status;
        String desired;
        String current;
        String upToDate;
        String ready;
        String available;

        @Override
        public void updateResource() {
            var ready = resource.getStatus().getNumberReady() == null ? 0 : resource.getStatus().getNumberReady();
            this.ready = String.valueOf(ready);
            this.desired = toStringOr0(resource.getStatus().getDesiredNumberScheduled());
            this.current = toStringOr0(resource.getStatus().getCurrentNumberScheduled());
            this.upToDate = toStringOr0(resource.getStatus().getUpdatedNumberScheduled());
            this.available = toStringOr0(resource.getStatus().getNumberAvailable());
            if (ready == 0)
                setColor(UiUtil.COLOR.GREY);
            else
                setColor(null);
        }

        private String toStringOr0(Integer integer) {
            if (integer == null) return "0";
            return integer.toString();
        }
    }
}
