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

package de.mhus.kt2l.resources.secret;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.aaa.UsersConfiguration.ROLE;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
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
public class ShowPodsUsingSecretAction implements ResourceAction {
    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return K8s.SECRET.equals(type);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {
        return canHandleType(cluster, type) && selected.size() == 1;
    }

    @Override
    public void execute(ExecutionContext context) {

        var parent = context.getSelected().stream().findFirst().get();
        final String parentName = parent.getMetadata().getName();
        final String parentNamespace = parent.getMetadata().getNamespace();
        final var uid = parent.getMetadata().getUid();
        ((ResourcesGridPanel)context.getSelectedTab().getPanel()).showResources(K8s.POD, K8sUtil.NAMESPACE_ALL, new ResourcesFilter() {
            @Override
            public boolean filter(KubernetesObject res) {
                if (res instanceof io.kubernetes.client.openapi.models.V1Pod pod) {
                    if (!pod.getMetadata().getNamespace().equals(parentNamespace)) return false;

                    var containers = pod.getSpec().getContainers();
                    if (containers != null) {
                        for (var container : containers) {
                            var env = container.getEnv();
                            if (env != null) {
                                for (var envVar : env) {
                                    if (envVar.getValueFrom() != null && envVar.getValueFrom().getSecretKeyRef() != null) {
                                        var ref = envVar.getValueFrom().getSecretKeyRef();
                                        if (ref.getName().equals(parentName)) return true;
                                    }
                                }
                            }
                            var envFrom = container.getEnvFrom();
                            if (envFrom != null) {
                                for (var envVarSource : envFrom) {
                                    if (envVarSource.getSecretRef() != null && envVarSource.getSecretRef().getName().equals(parentName)) return true;
                                }
                            }
                        }
                    }
                    var volumes = pod.getSpec().getVolumes();
                    if (volumes != null) {
                        for (var volume : volumes) {
                            if (volume.getSecret() != null && volume.getSecret().getSecretName().equals(parentName)) return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "Pods using Secret " + parentName;
            }
        }, null);
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
        return "CTRL+P";
    }

    @Override
    public String getDescription() {
        return "Show Pods using the selected Secret";
    }
}
