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

package de.mhus.kt2l.resources.hpa;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.commons.tools.MObject;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGridWithNamespace;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1HorizontalPodAutoscaler;
import io.kubernetes.client.openapi.models.V1HorizontalPodAutoscalerList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HorizontalPodAutoscalerGrid extends AbstractGridWithNamespace<HorizontalPodAutoscalerGrid.Resource, Component, V1HorizontalPodAutoscaler, V1HorizontalPodAutoscalerList> {

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return HorizontalPodAutoscalerWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(Resource::getReference).setHeader("Reference").setSortProperty("reference");
        resourcesGrid.addColumn(Resource::getTargets).setHeader("Targets").setSortable(false);
        resourcesGrid.addColumn(Resource::getMinPods).setHeader("Min Pods").setSortable(false);
        resourcesGrid.addColumn(Resource::getMaxPods).setHeader("Max Pods").setSortable(false);
        resourcesGrid.addColumn(Resource::getReplicas).setHeader("Replicas").setSortable(false);
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        return switch(sorted) {
            case ("reference") ->
                switch (direction) {
                    case ASCENDING -> MObject.compareTo(a.getReference(), b.getReference());
                    case DESCENDING -> MObject.compareTo(b.getReference(), a.getReference());
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
        return K8s.HPA;
    }

    @Getter
    public static class Resource extends ResourceItem<V1HorizontalPodAutoscaler> {
        String reference;
        String targets;
        int minPods;
        int maxPods;
        int replicas;

        @Override
        public void updateResource() {
            super.updateResource();
            reference = resource.getSpec().getScaleTargetRef().getKind() + "/" + resource.getSpec().getScaleTargetRef().getName();
            targets = resource.getStatus().getCurrentCPUUtilizationPercentage() + "% / " + resource.getSpec().getTargetCPUUtilizationPercentage() + "%";
            minPods = toIntOr0(resource.getSpec().getMinReplicas());
            maxPods = toIntOr0(resource.getSpec().getMaxReplicas());
        }

        private int toIntOr0(Integer integer) {
            if (integer == null) return 0;
            return integer;
        }
    }
}
