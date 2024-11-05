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

package de.mhus.kt2l.resources.common;

import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.aaa.UsersConfiguration.ROLE;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.ViewsConfiguration;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
@WithRole(ROLE.WRITE)
public class ActionDelete implements ResourceAction {

    @Autowired
    private ViewsConfiguration viewsConfiguration;

    @Autowired
    private K8sService k8s;
//    @Autowired
//    private PodK8s podHandler;

    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return !K8s.CONTAINER.equals(type) && !K8s.NODE.equals(type);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {
        return canHandleType(cluster, type) && selected.size() > 0;
    }

    @Override
    public void execute(ExecutionContext context) {

        final var config = viewsConfiguration.getConfig("resourcesDelete");
        final var action = new ActionDeleteDialog(context, config, k8s);
        action.open();

    }

    @Override
    public String getTitle() {
        return "Delete";
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.ACTIONS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.ACTIONS_ORDER + 100;
    }

    @Override
    public String getShortcutKey() {
        return "CONTROL+BACKSPACE";
    }

    @Override
    public String getDescription() {
        return "Delete pods or container";
    }

    @Override
    public AbstractIcon getIcon() {
        return VaadinIcon.FILE_REMOVE.create();
    }

}
