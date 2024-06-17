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
import de.mhus.commons.tools.MObject;
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
        resourcesGrid.addColumn(Resource::getStatus).setHeader("Status").setSortProperty("status");
        resourcesGrid.addColumn(Resource::getDesired).setHeader("Desired").setSortProperty("desired");
        resourcesGrid.addColumn(Resource::getCurrent).setHeader("Current").setSortProperty("current");
        resourcesGrid.addColumn(Resource::getReady).setHeader("Ready").setSortProperty("ready");
        resourcesGrid.addColumn(Resource::getUpToDate).setHeader("Up To Date").setSortProperty("upToDate");
        resourcesGrid.addColumn(Resource::getAvailable).setHeader("Available").setSortProperty("available");
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        return switch(sorted) {
            case ("status") ->
                switch (direction) {
                    case ASCENDING -> MObject.compareTo(a.getStatus(), b.getStatus());
                    case DESCENDING -> MObject.compareTo(b.getStatus(), a.getStatus());
                };
            case ("desired") ->
                switch (direction) {
                    case ASCENDING -> MObject.compareTo(a.getDesired(), b.getDesired());
                    case DESCENDING -> MObject.compareTo(b.getDesired(), a.getDesired());
                };
            case ("current") ->
                switch (direction) {
                    case ASCENDING -> MObject.compareTo(a.getCurrent(), b.getCurrent());
                    case DESCENDING -> MObject.compareTo(b.getCurrent(), a.getCurrent());
                };
            case ("ready") ->
                switch (direction) {
                    case ASCENDING -> MObject.compareTo(a.getReady(), b.getReady());
                    case DESCENDING -> MObject.compareTo(b.getReady(), a.getReady());
                };
            case ("upToDate") ->
                switch (direction) {
                    case ASCENDING -> MObject.compareTo(a.getUpToDate(), b.getUpToDate());
                    case DESCENDING -> MObject.compareTo(b.getUpToDate(), a.getUpToDate());
                };
            case ("available") ->
                switch (direction) {
                    case ASCENDING -> MObject.compareTo(a.getAvailable(), b.getAvailable());
                    case DESCENDING -> MObject.compareTo(b.getAvailable(), a.getAvailable());
                };
            default -> 0;
        };
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
        int desired;
        int current;
        int upToDate;
        int ready;
        int available;

        @Override
        public void updateResource() {
            super.updateResource();
            var ready = resource.getStatus().getNumberReady() == null ? 0 : resource.getStatus().getNumberReady();
            this.ready = toIntOr0(ready);
            this.desired = toIntOr0(resource.getStatus().getDesiredNumberScheduled());
            this.current = toIntOr0(resource.getStatus().getCurrentNumberScheduled());
            this.upToDate = toIntOr0(resource.getStatus().getUpdatedNumberScheduled());
            this.available = toIntOr0(resource.getStatus().getNumberAvailable());
            if (desired == 0)
                setColor(UiUtil.COLOR.GREY);
            else
            if (ready != desired)
                setColor(UiUtil.COLOR.RED);
            else
                setColor(null);
        }

        private int toIntOr0(Integer integer) {
            if (integer == null) return 0;
            return integer;
        }

    }
}
