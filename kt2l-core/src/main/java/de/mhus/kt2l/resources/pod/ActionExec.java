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

package de.mhus.kt2l.resources.pod;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.config.UsersConfiguration.ROLE;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@WithRole(ROLE.WRITE)
public class ActionExec implements ResourceAction {

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandleResourceType(K8s.RESOURCE resourceType) {
        return K8s.RESOURCE.POD.equals(resourceType) || K8s.RESOURCE.CONTAINER.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(K8s.RESOURCE resourceType, Set<? extends KubernetesObject> selected) {
        return canHandleResourceType(resourceType) && selected.size() > 0;
    }

    @Override
    public void execute(ExecutionContext context) {
        List<ContainerResource> containers = new ArrayList<>();

        if (context.getResourceType().equals(K8s.RESOURCE.CONTAINER)) {
            context.getSelected().forEach(c -> containers.add((ContainerResource)c));
        } else
        if (context.getResourceType().equals(K8s.RESOURCE.POD)) {
            context.getSelected().forEach(p -> {
                final var pod = (V1Pod)p;
                pod.getStatus().getContainerStatuses().forEach(cs -> {
                    containers.add(new ContainerResource(new PodGrid.Container(
                            PodGrid.CONTAINER_TYPE.DEFAULT,
                            cs,
                            pod)));
                });
                if (pod.getStatus().getEphemeralContainerStatuses() != null)
                    pod.getStatus().getEphemeralContainerStatuses().forEach(cs -> {
                        containers.add(new ContainerResource(new PodGrid.Container(
                                PodGrid.CONTAINER_TYPE.EPHEMERAL,
                                cs,
                                pod)));
                    });
                if (pod.getStatus().getInitContainerStatuses() != null)
                    pod.getStatus().getInitContainerStatuses().forEach(cs -> {
                        containers.add(new ContainerResource(new PodGrid.Container(
                                PodGrid.CONTAINER_TYPE.INIT,
                                cs,
                                pod)));
                    });
            });
        }

        final var selected = (V1Pod)context.getSelected().iterator().next();
        panelService.addPanel(
                context.getSelectedTab(),
                context.getCluster().getName() + ":" + selected.getMetadata().getNamespace() + "." + selected.getMetadata().getName() + ":exec",
                selected.getMetadata().getName(),
                false,
                VaadinIcon.FORWARD.create(),
                () ->
                        new PodExecPanel(
                                context.getCluster(),
                                context.getCore(),
                                containers
                        )).setHelpContext("exec").select();
    }

    @Override
    public String getTitle() {
        return "Exec;icon=" + VaadinIcon.FORWARD;
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.ACTIONS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.ACTIONS_ORDER + 10;
    }

    @Override
    public String getShortcutKey() {
        return "e";
    }

    @Override
    public String getDescription() {
        return "Execute command in container";
    }
}
