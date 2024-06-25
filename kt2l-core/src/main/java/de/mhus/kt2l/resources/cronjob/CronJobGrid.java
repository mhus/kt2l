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

package de.mhus.kt2l.resources.cronjob;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.commons.tools.MObject;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.ui.UiUtil;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGridWithNamespace;
import io.kubernetes.client.openapi.models.V1CronJob;
import io.kubernetes.client.openapi.models.V1CronJobList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class CronJobGrid extends AbstractGridWithNamespace<CronJobGrid.Resource, Component, V1CronJob, V1CronJobList> {

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return CronJobWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(Resource::getScheduled).setHeader("Scheduled").setSortProperty("scheduled");
        resourcesGrid.addColumn(Resource::getSuspend).setHeader("Suspend").setSortProperty("suspend");
        resourcesGrid.addColumn(Resource::getActive).setHeader("Active").setSortProperty("active");
        resourcesGrid.addColumn(Resource::getLastSchedule).setHeader("Last Scheduled").setSortProperty("lastSchedule");
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        return switch (sorted) {
            case "scheduled" -> switch (direction) {
                case ASCENDING -> MObject.compareTo(a.getScheduled(), b.getScheduled());
                case DESCENDING -> MObject.compareTo(b.getScheduled(), a.getScheduled());
            };
            case "suspend" -> switch (direction) {
                case ASCENDING -> MObject.compareTo(a.getSuspend(), b.getSuspend());
                case DESCENDING -> MObject.compareTo(b.getSuspend(), a.getSuspend());
            };
            case "active" -> switch (direction) {
                case ASCENDING -> MObject.compareTo(a.getActive(), b.getActive());
                case DESCENDING -> MObject.compareTo(b.getActive(), a.getActive());
            };
            case "lastSchedule" -> switch (direction) {
                case ASCENDING -> MObject.compareTo(a.getLastSchedule(), b.getLastSchedule());
                case DESCENDING -> MObject.compareTo(b.getLastSchedule(), a.getLastSchedule());
            };
            default -> 0;
        };
    }

    @Override
    protected Resource createResourceItem() {
        return new Resource();
    }

    @Override
    public K8s getManagedType() {
        return K8s.CRON_JOB;
    }

    @Getter
    public static class Resource extends ResourceItem<V1CronJob> { String desired;
        private String scheduled;
        private Boolean suspend;
        private int active;
        private OffsetDateTime lastSchedule;

        @Override
        public void updateResource() {
            super.updateResource();
            this.scheduled = resource.getSpec().getSchedule();
            this.suspend = resource.getSpec().getSuspend();
            this.active = tryThis(() -> resource.getStatus().getActive().size()).or(0);
            this.lastSchedule = resource.getStatus().getLastScheduleTime();

            if (suspend != null && suspend) {
                setColor(UiUtil.COLOR.GREY);
            } else
                setColor(null);
        }

        private String toStringOr0(Integer integer) {
            if (integer == null) return "0";
            return integer.toString();
        }
    }
}
