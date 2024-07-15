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

import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.aaa.UsersConfiguration.ROLE;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.resources.ResourcesFilter;
import de.mhus.kt2l.resources.ResourcesGridPanel;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@WithRole(ROLE.READ)
public class ShowPodsOfStatefulSetAction implements ResourceAction {
    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return K8s.STATEFUL_SET.equals(type);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {
        return canHandleType(cluster, type) && selected.size() == 1;
    }

    @Override
    public void execute(ExecutionContext context) {

        var parent = context.getSelected().stream().findFirst().get();
        final String parentName = parent.getMetadata().getName();
        final var uid = parent.getMetadata().getUid();
        ((ResourcesGridPanel)context.getSelectedTab().getPanel()).showResources(K8s.POD, parent.getMetadata().getNamespace(), new ResourcesFilter() {
            @Override
            public boolean filter(KubernetesObject res) {
                if (res instanceof io.kubernetes.client.openapi.models.V1Pod pod) {
                    var ownerReferences = pod.getMetadata().getOwnerReferences();
                    if (ownerReferences != null) {
                        for (var ref : ownerReferences)
                            if (ref.getUid().equals(uid)) return true;
                    }
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "Pods of StatefulSet " + parentName;
            }
        }, null);
    }

    @Override
    public String getTitle() {
        return "Pods";
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
        return "CTRL+P";
    }

    @Override
    public String getDescription() {
        return "Show Pods of the selected StatefulSet";
    }

    @Override
    public AbstractIcon getIcon() {
        return VaadinIcon.OPEN_BOOK.create();
    }
}
