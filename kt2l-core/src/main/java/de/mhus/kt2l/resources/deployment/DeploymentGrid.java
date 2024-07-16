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

package de.mhus.kt2l.resources.deployment;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.commons.tools.MObject;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.pod.PodGrid;
import de.mhus.kt2l.resources.util.AbstractGridWithNamespace;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeploymentGrid extends AbstractGridWithNamespace<DeploymentGrid.Resource, DeploymentRolloutPanel, V1Deployment, V1DeploymentList> {

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return DeploymentWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return DeploymentGrid.Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(DeploymentGrid.Resource::getStatus).setHeader("Status").setSortProperty("status");
//        resourcesGrid.addColumn(DeploymentGrid.Resource::getReplicas).setHeader("Replicas").setSortProperty("replicas");
        resourcesGrid.addColumn(DeploymentGrid.Resource::getDesired).setHeader("Desired").setSortProperty("desired");
        resourcesGrid.addColumn(DeploymentGrid.Resource::getCurrent).setHeader("Current").setSortProperty("current");
        resourcesGrid.addColumn(DeploymentGrid.Resource::getUpToDate).setHeader("Up-To-Date").setSortProperty("uptodate");
        resourcesGrid.addColumn(DeploymentGrid.Resource::getAvailable).setHeader("Available").setSortProperty("available");
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        return switch(sorted) {
            case ("status") -> switch(direction) {
                case ASCENDING -> MObject.compareTo(a.getStatus(), b.getStatus());
                case DESCENDING -> MObject.compareTo(b.getStatus(), a.getStatus());
            };
//            case ("replicas") -> switch(direction) {
//                case ASCENDING -> MObject.compareTo(a.getReplicas(), b.getReplicas());
//                case DESCENDING -> MObject.compareTo(b.getReplicas(), a.getReplicas());
//            };
            case ("desired") -> switch(direction) {
                case ASCENDING -> MObject.compareTo(a.getDesired(), b.getDesired());
                case DESCENDING -> MObject.compareTo(b.getDesired(), a.getDesired());
            };
            case ("current") -> switch(direction) {
                case ASCENDING -> MObject.compareTo(a.getCurrent(), b.getCurrent());
                case DESCENDING -> MObject.compareTo(b.getCurrent(), a.getCurrent());
            };
            case ("uptodate") -> switch(direction) {
                case ASCENDING -> MObject.compareTo(a.getUpToDate(), b.getUpToDate());
                case DESCENDING -> MObject.compareTo(b.getUpToDate(), a.getUpToDate());
            };
            case ("available") -> switch(direction) {
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
    public V1APIResource getManagedType() {
        return K8s.DEPLOYMENT;
    }

    protected void createDetailsComponent() {
        detailsComponent = new DeploymentRolloutPanel(panel.getCore(), cluster, (DeploymentK8s) resourceHandler);
        detailsComponent.setVisible(false);
    }

    @Override
    protected void onDetailsChanged(DeploymentGrid.Resource item) {
        onGridCellFocusChanged(item);
    }

    @Override
    protected void onShowDetails(DeploymentGrid.Resource item, boolean flip) {
        detailsComponent.cleanTarget();
        detailsComponent.setVisible(!flip || !detailsComponent.isVisible());
        if (detailsComponent.isVisible()) {
            detailsComponent.setTarget(item.getResource());
        }
    }

    @Override
    protected void onGridSelectionChanged() {
    }

    @Override
    protected void onGridCellFocusChanged(DeploymentGrid.Resource item) {
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
    public static class Resource extends ResourceItem<V1Deployment> {
        String status;
//        String replicas;
        private int desired;
        private int current;
        private int upToDate;
        private int available;

        @Override
        public void updateResource() {
            super.updateResource();
            int ready = resource.getStatus().getReadyReplicas() == null ? 0 : resource.getStatus().getReadyReplicas();
            int replicas = resource.getStatus().getReplicas() == null ? 0 : resource.getStatus().getReplicas();
//            this.replicas = ready + "/" + replicas;
            if (replicas == 0)
                status = "Empty";
            else
            if (ready == replicas)
                status = "Ready";
            else
                status = "Not Ready";

            desired = toIntOr0(resource.getSpec().getReplicas());
            current = toIntOr0(resource.getStatus().getReplicas());
            upToDate = toIntOr0(resource.getStatus().getUpdatedReplicas());
            available = toIntOr0(resource.getStatus().getAvailableReplicas());

            if (ready != replicas)
                setColor(UiUtil.COLOR.RED);
            else if (replicas == 0)
                setColor(UiUtil.COLOR.GREY);
            else
                setColor(null);
        }

        private int toIntOr0(Integer value) {
            return value == null ? 0 : value;
        }
    }

}
