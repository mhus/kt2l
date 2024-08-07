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

import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.Cluster;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;

import java.util.Set;

public interface ResourceAction {
    int ACTIONS_ORDER = 1000;
    String ACTIONS_PATH = "Actions;icon=" + VaadinIcon.START_COG;
    int VIEW_ORDER = 2000;
    String VIEW_PATH = "View;icon=" + VaadinIcon.EYE;
    int EDIT_ORDER = 3000;
    String EDIT_PATH = "Edit;icon=" + VaadinIcon.EDIT;
    int TOOLS_ORDER = 5000;
    String TOOLS_PATH = "Tools;icon=" + VaadinIcon.TOOLS;

    boolean canHandleType(Cluster cluster, V1APIResource type);
    boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected);
    void execute(ExecutionContext context);
    String getTitle();
    String getMenuPath();
    int getMenuOrder();
    String getShortcutKey();

    String getDescription();

    AbstractIcon getIcon();

}
