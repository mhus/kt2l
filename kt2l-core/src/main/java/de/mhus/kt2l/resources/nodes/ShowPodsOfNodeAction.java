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

package de.mhus.kt2l.resources.nodes;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.config.UsersConfiguration.ROLE;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.resources.ResourcesFilter;
import de.mhus.kt2l.resources.ResourcesGridPanel;
import io.kubernetes.client.common.KubernetesObject;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@WithRole(ROLE.READ)
public class ShowPodsOfNodeAction implements ResourceAction {
    @Override
    public boolean canHandleResourceType(K8s.RESOURCE resourceType) {
        return K8s.RESOURCE.NODE.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(K8s.RESOURCE resourceType, Set<? extends KubernetesObject> selected) {
        return canHandleResourceType(resourceType) && selected.size() == 1;
    }

    @Override
    public void execute(ExecutionContext context) {

        final String nodeName = context.getSelected().iterator().next().getMetadata().getName();
        ((ResourcesGridPanel)context.getSelectedTab().getPanel()).showResources(K8s.RESOURCE.POD, new ResourcesFilter() {
            @Override
            public boolean filter(KubernetesObject res) {
                if (res instanceof io.kubernetes.client.openapi.models.V1Pod pod) {
                    return pod.getSpec().getNodeName().equals(nodeName);
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "Pods on node " + nodeName;
            }
        });
    }

    @Override
    public String getTitle() {
        return "Pods;icon=" + VaadinIcon.OPEN_BOOK;
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.VIEW_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.VIEW_ORDER + 110;
    }

    @Override
    public String getShortcutKey() {
        return "P";
    }

    @Override
    public String getDescription() {
        return "Show pods of the selected node";
    }
}
