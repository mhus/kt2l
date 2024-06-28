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
package de.mhus.kt2l.resources.service;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.config.ViewsConfiguration;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.portforward.PortForwardingPanel;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@WithRole({UsersConfiguration.ROLE.WRITE,UsersConfiguration.ROLE.LOCAL})
public class OpenSvcPortForwardAction implements ResourceAction {

    @Autowired
    private PanelService panelService;

    @Autowired
    private ViewsConfiguration viewsConfiguration;

    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return K8s.SERVICE.equals(type);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {
        if (!canHandleType(cluster, type))
            return false;
        if (selected.isEmpty()) return false;

        for (KubernetesObject obj : selected) {
            if (obj instanceof V1Service service) {
                if (service.getSpec().getSelector() != null)
                    return true;
            }
        }

        return false;
    }

    @Override
    public void execute(ExecutionContext context) {

        // var portForwarder = context.getCore().backgroundJobInstance(context.getCluster(), PortForwardBackgroundJob.class);

        AtomicInteger nextPort = new AtomicInteger(viewsConfiguration.getConfig("portForward").getInt("firstPort", 9000));
        var selected = context.getSelected();
        StringBuilder cmd = new StringBuilder();

        for (KubernetesObject obj : selected) {

            if (obj instanceof V1Service service) {
                service.getSpec().getPorts().forEach(port -> {
                    cmd
                            .append("svc ")
                            .append(service.getMetadata().getNamespace()).append(" ")
                            .append(service.getMetadata().getName()).append(" ")
                            .append(port.getPort()).append(" ")
                            .append(nextPort.getAndIncrement())
                            .append("\n");
                });
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
