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
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.config.ViewsConfiguration;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.portforward.PortForwardingPanel;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@WithRole(UsersConfiguration.ROLE.WRITE)
public class OpenPodPortForwardAction implements ResourceAction {

    @Autowired
    private PanelService panelService;

    @Autowired
    private ViewsConfiguration viewsConfiguration;

    @Override
    public boolean canHandleResourceType(Cluster cluster, K8s resourceType) {
        return K8s.POD.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, K8s resourceType, Set<? extends KubernetesObject> selected) {
        if (!canHandleResourceType(cluster, resourceType))
            return false;
        if (selected.isEmpty()) return false;

        for (KubernetesObject obj : selected) {
            if (obj instanceof V1Pod pod) {
                if (pod.getSpec().getContainers() != null) {
                    for (var container : pod.getSpec().getContainers()) {
                        if (container.getPorts() != null && !container.getPorts().isEmpty()) {
                            return true;
                        }
                    }
                }
            } else
            if (obj instanceof ContainerResource container) {
                if (container.getContainer().getPorts() != null && !container.getContainer().getPorts().isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void execute(ExecutionContext context) {

        AtomicInteger nextPort = new AtomicInteger(viewsConfiguration.getConfig("portForward").getInt("firstPort", 9000));
        var selected = context.getSelected();
        StringBuilder cmd = new StringBuilder();


        for (KubernetesObject obj : selected) {
            if (obj instanceof V1Pod pod) {
                if (pod.getSpec().getContainers() != null) {
                    for (var container : pod.getSpec().getContainers()) {
                        if (container.getPorts() != null && !container.getPorts().isEmpty()) {
                            container.getPorts().forEach(port -> {
                                cmd
                                        .append("pod ")
                                        .append(pod.getMetadata().getNamespace()).append(" ")
                                        .append(pod.getMetadata().getName()).append(" ")
                                        .append(port.getContainerPort()).append(" ")
                                        .append(nextPort.getAndIncrement())
                                        .append("\n");
                            });
                        }
                    }
                }
            } else
            if (obj instanceof ContainerResource container) {
                if (container.getContainer().getPorts() != null && !container.getContainer().getPorts().isEmpty()) {
                    container.getContainer().getPorts().forEach(port -> {
                        cmd
                                .append("pod ")
                                .append(container.getPod().getMetadata().getNamespace()).append(" ")
                                .append(container.getPod().getMetadata().getName()).append(" ")
                                .append(port.getContainerPort()).append(" ")
                                .append(nextPort.getAndIncrement())
                                .append("\n");
                    });
                }
            }
        }

        var tab = panelService.showPortForwardingPanel(context.getCore(), context.getCluster()).select();
        ((PortForwardingPanel)tab.getPanel()).setCommand(cmd.toString());

    }

    @Override
    public String getTitle() {
        return "Port Forward;icon=" + VaadinIcon.CLOUD_UPLOAD_O.name();
    }

    @Override
    public String getMenuPath() {
        return ACTIONS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return 3263;
    }

    @Override
    public String getShortcutKey() {
        return "P";
    }

    @Override
    public String getDescription() {
        return "Open port forwarder panel and prepare the command";
    }
}
