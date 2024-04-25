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

package de.mhus.kt2l.resources.pods;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import de.mhus.commons.tools.MCollection;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tools.MSystem;
import de.mhus.commons.tree.MProperties;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.config.CmdConfiguration;
import de.mhus.kt2l.config.ConfigUtil;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.config.ShellConfiguration;
import de.mhus.kt2l.config.UsersConfiguration.ROLE;
import de.mhus.kt2l.core.UiUtil;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.core.WithRole;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Slf4j
@Component
@WithRole({ROLE.WRITE, ROLE.LOCAL})
public class ActionTerminal implements ResourceAction {

    @Autowired
    private CmdConfiguration cmdConfiguration;
    @Autowired
    private ShellConfiguration shellConfiguration;


    @Override
    public boolean canHandleResourceType(String resourceType) {
        return
                K8sUtil.RESOURCE_PODS.equals(resourceType) || K8sUtil.RESOURCE_CONTAINER.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(String resourceType, Set<? extends KubernetesObject> selected) {
        return canHandleResourceType(resourceType) && selected.size() == 1;
    }

    @Override
    public void execute(ExecutionContext context) {

        V1Pod pod = null;
        String container = null;
        String containerImage = null;
        if (K8sUtil.RESOURCE_PODS.equals(context.getResourceType())) {
            pod = (V1Pod) context.getSelected().iterator().next();
            container = pod.getStatus().getContainerStatuses().get(0).getName();
            containerImage = pod.getStatus().getContainerStatuses().get(0).getImage();
        } else {
            var containerResource = (ContainerResource)context.getSelected().iterator().next();
            pod = containerResource.getPod();
            container = containerResource.getContainerName();
            final String finalContainer = container;
            containerImage = containerResource.getPod().getStatus().getContainerStatuses().stream().filter(c -> c.getName().equals(finalContainer)).findFirst().get().getImage();
        }

        final var shell =  shellConfiguration.getShellFor(context.getClusterConfiguration(), pod, containerImage);
        final var vars = new MProperties();
        vars.setString("pod", pod.getMetadata().getName());
        vars.setString("container", container);
        vars.setString("namespace", pod.getMetadata().getNamespace());
        vars.setString("context", context.getClusterConfiguration().name());
        vars.setString("cmd", shell);

        cmdConfiguration.execute("exec", vars);

    }

    @Override
    public String getTitle() {
        return "Terminal;icon=" + VaadinIcon.TERMINAL;
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.ACTIONS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.ACTIONS_ORDER+30;
    }

    @Override
    public String getShortcutKey() {
        return "t";
    }

    @Override
    public String getDescription() {
        return "Open shell in terminal";
    }
}
