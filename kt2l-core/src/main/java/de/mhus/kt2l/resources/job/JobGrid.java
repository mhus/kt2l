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

package de.mhus.kt2l.resources.job;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.UiUtil;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.util.AbstractGridWithNamespace;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class JobGrid extends AbstractGridWithNamespace<JobGrid.Resource, Component, V1Job, V1JobList> {

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return JobWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(Resource::getActive).setHeader("Active").setSortable(false);
        resourcesGrid.addColumn(Resource::getReady).setHeader("Ready").setSortable(false);
        resourcesGrid.addColumn(Resource::getFailed).setHeader("Failed").setSortable(false);
        resourcesGrid.addColumn(Resource::getSucceeded).setHeader("Succseeded").setSortable(false);
        resourcesGrid.addColumn(Resource::getCompletions).setHeader("Completions").setSortable(false);
        resourcesGrid.addColumn(Resource::getDuration).setHeader("Duration").setSortable(false);
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        return 0;
    }

    @Override
    protected Resource createResourceItem() {
        return new Resource();
    }

    @Override
    public K8s getManagedResourceType() {
        return K8s.JOB;
    }

    @Getter
    public static class Resource extends ResourceItem<V1Job> {
        String ready;
        private String completed;
        private String duration;
        private String active;
        private String failed;
        private String succeeded;
        private String completions;

        @Override
        public void updateResource() {
            super.updateResource();
            this.completed = resource.getStatus().getCompletedIndexes();
            this.duration = tryThis(() -> K8sUtil.getAgeSeconds(resource.getStatus().getCompletionTime().toEpochSecond() - resource.getStatus().getStartTime().toEpochSecond())).or("");
            this.ready = toStringOr0(resource.getStatus().getReady());
            this.active = toStringOr0(resource.getStatus().getActive());
            this.failed = toStringOr0(resource.getStatus().getFailed());
            this.succeeded = toStringOr0(resource.getStatus().getSucceeded());
            this.completions = toStringOr0(resource.getSpec().getCompletions());
            if (!failed.equals("0"))
                setColor(UiUtil.COLOR.RED);
            else if (active.equals("0"))
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
