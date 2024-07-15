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

import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.resources.ResourcesFilter;
import de.mhus.kt2l.resources.ResourcesGridPanel;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1ClusterRoleBinding;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@WithRole(UsersConfiguration.ROLE.READ)
public class ShowServiceAccountsForClusterRoleBindingAction implements ResourceAction {
    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return K8s.CLUSTER_ROLE_BINDING.equals(type);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {
        return canHandleType(cluster, type) && selected.size() == 1;
    }

    @Override
    public void execute(ExecutionContext context) {
        var source = (V1ClusterRoleBinding)context.getSelected().iterator().next();
        final var name = source.getMetadata().getName();
        final var namespace = source.getMetadata().getNamespace();
        final var serviceAccounts = source.getSubjects().stream().filter(s -> "ServiceAccount".equals(s.getKind())).map(s -> s.getNamespace() + "." + s.getName()).collect(Collectors.toSet());

        ((ResourcesGridPanel)context.getSelectedTab().getPanel()).showResources(K8s.SERVICE_ACCOUNT, namespace, new ResourcesFilter() {
            @Override
            public boolean filter(KubernetesObject res) {
                if (res instanceof io.kubernetes.client.openapi.models.V1ServiceAccount serviceAccount) {
                    return serviceAccounts.contains(serviceAccount.getMetadata().getNamespace() + "." + serviceAccount.getMetadata().getName());
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "Service Accounts for Cluster Role Binding " + name;
            }
        }, null);
    }

    @Override
    public String getTitle() {
        return "Service Accounts";
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
        return "CTRL+S";
    }

    @Override
    public String getDescription() {
        return "Show Service Accounts for Role Binding";
    }

    @Override
    public AbstractIcon getIcon() {
        return VaadinIcon.ARROW_FORWARD.create();
    }
}
