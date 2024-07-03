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
import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1Secret;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@WithRole(UsersConfiguration.ROLE.WRITE)
public class EditSecretAction implements ResourceAction {

    @Autowired
    private PanelService panelService;

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
        var selected = context.getSelected().iterator().next();
        panelService.showEditSecretPanel(context.getSelectedTab(), context.getCore(), context.getCluster(), (V1Secret) selected).select();
    }

    @Override
    public String getTitle() {
        return "Edit;icon=" + VaadinIcon.PASSWORD.name();
    }

    @Override
    public String getMenuPath() {
        return ACTIONS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return 2012;
    }

    @Override
    public String getShortcutKey() {
        return "E";
    }

    @Override
    public String getDescription() {
        return "Edit Secret content";
    }
}
