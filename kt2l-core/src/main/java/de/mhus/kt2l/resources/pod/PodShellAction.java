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
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.config.UsersConfiguration.ROLE;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@WithRole(ROLE.WRITE)
public class PodShellAction implements ResourceAction {

    @Autowired
    private Configuration configuration;

    @Autowired
    private PanelService panelService;


    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return
                K8s.POD.equals(type) || K8s.CONTAINER.equals(type);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {
        return canHandleType(cluster, type) && selected.size() == 1;
    }

    @Override
    public void execute(ExecutionContext context) {
        var selected = (V1Pod)context.getSelected().iterator().next();
        panelService.showContainerShellPanel(context.getSelectedTab(), context.getCluster(), context.getCore(), selected).select();
    }

    @Override
    public String getTitle() {
        return "Shell;icon=" + VaadinIcon.TERMINAL;
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.ACTIONS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.ACTIONS_ORDER+20;
    }

    @Override
    public String getShortcutKey() {
        return "s";
    }

    @Override
    public String getDescription() {
        return "Open shell";
    }
}
