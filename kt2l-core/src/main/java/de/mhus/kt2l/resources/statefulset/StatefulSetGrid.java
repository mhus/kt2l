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

package de.mhus.kt2l.resources.statefulset;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.deployment.DeploymentGrid;
import de.mhus.kt2l.resources.deployment.DeploymentK8s;
import de.mhus.kt2l.resources.deployment.DeploymentRolloutPanel;
import de.mhus.kt2l.resources.util.AbstractGridWithNamespace;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1StatefulSetList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StatefulSetGrid extends AbstractGridWithNamespace<StatefulSetGrid.Resource, StatefulSetRolloutPanel, V1StatefulSet, V1StatefulSetList> {

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return StatefulSetWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(Resource::getStatus).setHeader("Status").setSortProperty("status");
        resourcesGrid.addColumn(Resource::getReplicas).setHeader("Replicas").setSortProperty("replicas");
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        if ("status".equals(sorted)) {
            switch (direction) {
                case ASCENDING: return a.getStatus().compareTo(b.getStatus());
                case DESCENDING: return b.getStatus().compareTo(a.getStatus());
            }
        }
        if ("replicas".equals(sorted)) {
            switch (direction) {
                case ASCENDING: return a.getReplicas().compareTo(b.getReplicas());
                case DESCENDING: return b.getReplicas().compareTo(a.getReplicas());
            }
        }
        return 0;
    }

    @Override
    protected Resource createResourceItem() {
        return new Resource();
    }

    @Override
    public V1APIResource getManagedType() {
        return K8s.STATEFUL_SET;
    }

    protected void createDetailsComponent() {
        detailsComponent = new StatefulSetRolloutPanel(panel.getCore(), cluster);
        detailsComponent.setVisible(false);
    }

    @Override
    protected void onDetailsChanged(Resource item) {
        onGridCellFocusChanged(item);
    }

    @Override
    protected void onShowDetails(Resource item, boolean flip) {
        detailsComponent.cleanTarget();
        detailsComponent.setVisible(!flip || !detailsComponent.isVisible());
        if (detailsComponent.isVisible()) {
            detailsComponent.setHandler((DeploymentK8s) resourceHandler);
            detailsComponent.setTarget(item.getResource());
        }
    }

    @Override
    protected void onGridSelectionChanged() {
    }

    @Override
    protected void onGridCellFocusChanged(Resource item) {
        if (detailsComponent.isVisible()) {
            detailsComponent.setTarget(item.getResource());
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (detailsComponent != null) {
            detailsComponent.close();
        }
    }

    @Getter
    public static class Resource extends ResourceItem<V1StatefulSet> {
        String status;
        String replicas;

        @Override
        public void updateResource() {
            super.updateResource();
            int ready = resource.getStatus().getReadyReplicas() == null ? 0 : resource.getStatus().getReadyReplicas();
            int replicas = resource.getStatus().getReplicas() == null ? 0 : resource.getStatus().getReplicas();
            this.replicas = ready + "/" + replicas;
            if (replicas == 0)
                status = "Empty";
            else
            if (ready == replicas)
                status = "Ready";
            else
                status = "Not Ready";
            if (ready != replicas)
                setColor(UiUtil.COLOR.RED);
            else if (replicas == 0)
                setColor(UiUtil.COLOR.GREY);
            else
                setColor(null);
        }
    }
}
