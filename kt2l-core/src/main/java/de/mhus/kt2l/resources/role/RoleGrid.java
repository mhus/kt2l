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

package de.mhus.kt2l.resources.role;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractGridWithNamespace;
import io.kubernetes.client.openapi.models.V1Role;
import io.kubernetes.client.openapi.models.V1RoleList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class RoleGrid extends AbstractGridWithNamespace<RoleGrid.Resource, Component, V1Role, V1RoleList> {

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return RoleWatch.class;
    }

    @Override
    protected Class<Resource> getManagedResourceItemClass() {
        return Resource.class;
    }

    @Override
    protected void createGridColumnsAfterName(Grid<Resource> resourcesGrid) {
        resourcesGrid.addColumn(Resource::getResources).setHeader("Resources").setSortable(false);
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
    public K8s getManagedType() {
        return K8s.ROLE;
    }

    @Getter
    public static class Resource extends ResourceItem<V1Role> {
        String resources;

        @Override
        public void updateResource() {
            super.updateResource();
            resources = tryThis(()-> resource.getRules().stream().map(rule -> rule.getResources().toString()).reduce("", (a, b) -> a + b)).or("");
        }

        private String toStringOr0(Integer integer) {
            if (integer == null) return "0";
            return integer.toString();
        }
    }
}
