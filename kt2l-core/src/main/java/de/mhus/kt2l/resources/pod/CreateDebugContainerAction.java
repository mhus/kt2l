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

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1EphemeralContainer;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@WithRole(UsersConfiguration.ROLE.ADMIN)
public class CreateDebugContainerAction implements ResourceAction {
    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return K8s.POD.equals(type) && cluster.isVersionOrHigher(1,25);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {
        return canHandleType(cluster, type) && selected.size() > 0;
    }

    @Override
    public void execute(ExecutionContext context) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setText("Create debug container in selected pod?");
        dialog.setConfirmText("Create");
        dialog.setCancelText("Cancel");
        dialog.setCancelable(true);
        dialog.setCloseOnEsc(true);
        dialog.setConfirmButtonTheme("primary");
        dialog.setCancelButtonTheme("tertiary");
        dialog.addConfirmListener(e -> createDebugContainer(context));
        dialog.open();

    }

    private void createDebugContainer(ExecutionContext context) {
        context.getSelected().forEach(pod -> createDebugContainer(context, (V1Pod)pod));
    }

    private void createDebugContainer(ExecutionContext context, V1Pod pod) {
        try {
            if (pod.getSpec().getEphemeralContainers() == null)
                pod.getSpec().setEphemeralContainers(new ArrayList<>());
            var container = new V1EphemeralContainer();
            var name = "debugger-" + UUID.randomUUID().toString();
            container.setName(name);
            container.setImage("busybox");
            container.setCommand(List.of("sh"));
            container.setStdin(true);
            container.setTty(true);
            pod.getSpec().getEphemeralContainers().add(container);
            context.getCluster().getApiProvider().getCoreV1Api().replaceNamespacedPodEphemeralcontainers(pod.getMetadata().getName(), pod.getMetadata().getNamespace(), pod).execute();
            UiUtil.showSuccessNotification("Created " + name + " in " + pod.getMetadata().getName() + " pod");
        } catch (Exception e) {
            LOGGER.warn("Create debug container failed", e);
            context.getErrors().add(e);
        }
    }

    @Override
    public String getTitle() {
        return "Create Debug Container";
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.TOOLS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.TOOLS_ORDER + 100;
    }

    @Override
    public String getShortcutKey() {
        return "Ctrl+D";
    }

    @Override
    public String getDescription() {
        return "Create an ephemeral debug container in the selected pod";
    }

    @Override
    public AbstractIcon getIcon() {
        return VaadinIcon.ENVELOPE_OPEN_O.create();
    }
}
