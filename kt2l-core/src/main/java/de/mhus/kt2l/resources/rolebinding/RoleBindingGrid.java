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

package de.mhus.kt2l.resources.rolebinding;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.commons.tools.MObject;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGridWithNamespace;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1RoleBinding;
import io.kubernetes.client.openapi.models.V1RoleBindingList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class RoleBindingGrid extends AbstractGridWithNamespace<RoleBindingGrid.Resource, Component, V1RoleBinding, V1RoleBindingList> {

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return RoleBindingWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(Resource::getRole).setHeader("Role").setSortProperty("role");
        resourcesGrid.addColumn(Resource::getKind).setHeader("Kind").setSortProperty("kind");
        resourcesGrid.addColumn(Resource::getSubjects).setHeader("Subjects").setSortable(false);
    }

    @Override
    protected int sortColumn(String sorted, SortDirection direction, Resource a, Resource b) {
        return switch(sorted) {
            case ("role") ->
                switch (direction) {
                    case ASCENDING -> MObject.compareTo(a.getRole(), b.getRole());
                    case DESCENDING -> MObject.compareTo(b.getRole(), a.getRole());
                };
            case ("kind") ->
                switch (direction) {
                    case ASCENDING -> MObject.compareTo(a.getKind(), b.getKind());
                    case DESCENDING -> MObject.compareTo(b.getKind(), a.getKind());
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
        return K8s.ROLE_BINDING;
    }

    @Getter
    public static class Resource extends ResourceItem<V1RoleBinding> {
        String role;
        String kind;
        String subjects;

        @Override
        public void updateResource() {
            super.updateResource();
            role = tryThis(() -> resource.getRoleRef().getName()).orElse("");
            kind = tryThis(() -> resource.getRoleRef().getKind()).orElse("");
            subjects = tryThis(() -> resource.getSubjects().stream().map(s -> s.getName()).reduce((a, b) -> a + ", " + b).orElse("")).orElse("");
        }

        private String toStringOr0(Integer integer) {
            if (integer == null) return "0";
            return integer.toString();
        }
    }
}
