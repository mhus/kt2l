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
package de.mhus.kt2l.resources.persistentvolumeclaim;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.resources.ResourcesFilter;
import de.mhus.kt2l.resources.ResourcesGridPanel;
import io.kubernetes.client.common.KubernetesObject;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@WithRole(UsersConfiguration.ROLE.READ)
public class ShowVolumesForClaimAction implements ResourceAction {
    @Override
    public boolean canHandleResourceType(Cluster cluster, K8s resourceType) {
        return K8s.PERSISTENT_VOLUME_CLAIM.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, K8s resourceType, Set<? extends KubernetesObject> selected) {
        return canHandleResourceType(cluster, resourceType) && selected.size() == 1;
    }

    @Override
    public void execute(ExecutionContext context) {
        var source = context.getSelected().iterator().next();
        final var name = source.getMetadata().getName();

        ((ResourcesGridPanel)context.getSelectedTab().getPanel()).showResources(K8s.PERSISTENT_VOLUME, K8sUtil.NAMESPACE_ALL, new ResourcesFilter() {
            @Override
            public boolean filter(KubernetesObject res) {
                if (res instanceof io.kubernetes.client.openapi.models.V1PersistentVolume volume) {
                    return volume.getSpec().getClaimRef().getName().equals(name);
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "Volumes for Claim " + name;
            }
        }, null);
    }

    @Override
    public String getTitle() {
        return "Volumes;icon=" + VaadinIcon.ARROW_FORWARD;
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.VIEW_PATH;
    }

    @Override
    public int getMenuOrder() {
        return 1234;
    }

    @Override
    public String getShortcutKey() {
        return "CTRL+V";
    }

    @Override
    public String getDescription() {
        return "Show volumes for claim";
    }
}
