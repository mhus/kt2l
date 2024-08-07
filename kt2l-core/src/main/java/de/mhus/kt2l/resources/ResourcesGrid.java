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

package de.mhus.kt2l.resources;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.grid.GridSortOrder;
import de.mhus.kt2l.cluster.Cluster;
import io.kubernetes.client.openapi.models.V1APIResource;

import java.util.List;

public interface ResourcesGrid {

    Component getComponent();

    void refresh(long counter);

    void init(Cluster cluster, ResourcesGridPanel view);

    void setFilter(String value, ResourcesFilter resourcesFilter);

    void setNamespace(String value);

    String getNamespace();

    void setType(V1APIResource type);

    void handleShortcut(ShortcutEvent event);

    void setSelected();

    void setUnselected();

    void destroy();

    boolean isNamespaced();

    <T> List<GridSortOrder<T>> getSortOrder();

    void setSortOrder(String sortOrder, boolean sortAscending);

    default List<ResourceFilterFactory> getResourceFilterFactories() {
        return null;
    }

}