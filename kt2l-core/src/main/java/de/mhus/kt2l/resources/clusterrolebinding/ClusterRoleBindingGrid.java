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

package de.mhus.kt2l.resources.clusterrolebinding;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGridWithoutNamespace;
import io.kubernetes.client.openapi.models.V1ClusterRoleBinding;
import io.kubernetes.client.openapi.models.V1ClusterRoleBindingList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class ClusterRoleBindingGrid extends AbstractGridWithoutNamespace<ClusterRoleBindingGrid.Resource, Component, V1ClusterRoleBinding, V1ClusterRoleBindingList> {

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return ClusterRoleBindingWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(Resource::getClusterRole).setHeader("Cluster Role").setSortable(false);
        resourcesGrid.addColumn(Resource::getSubjectKind).setHeader("Subject Kind").setSortable(false);
        resourcesGrid.addColumn(Resource::getSubjects).setHeader("Subjects").setSortable(false);
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
        return K8s.CLUSTER_ROLE_BINDING;
    }

    @Getter
    public static class Resource extends ResourceItem<V1ClusterRoleBinding> {
        String clusterRole;
        String subjectKind;
        String subjects;

        @Override
        public void updateResource() {
            super.updateResource();
            clusterRole = tryThis(() -> resource.getRoleRef().getName()).or("");
            subjectKind = tryThis(() -> resource.getSubjects().get(0).getKind()).or("");
            subjects = tryThis(() -> resource.getSubjects().stream().map(s -> s.getName()).reduce((a, b) -> a + ", " + b).get()).or("");
        }
    }
}
