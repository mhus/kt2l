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
package de.mhus.kt2l.resources.deployment;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.IntegerField;
import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.ui.ProgressDialog;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1Deployment;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@WithRole(UsersConfiguration.ROLE.WRITE)
public class ScaleDeploymentAction implements ResourceAction {
    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return K8s.DEPLOYMENT.equals(type);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {
        return canHandleType(cluster, type) && selected.size() > 0;
    }

    @Override
    public void execute(ExecutionContext context) {

        Dialog dialog = new Dialog();
        dialog.setWidth("400px");
        dialog.setHeight("400px");
        dialog.setHeaderTitle("Scale Deployment");
        IntegerField replicasField = new IntegerField();
        replicasField.setLabel("Replicas");
        replicasField.setMin(0);
        replicasField.setValue( ((V1Deployment)context.getSelected().stream().findFirst().get()).getSpec().getReplicas() );
        replicasField.setStepButtonsVisible(true);
        dialog.add(replicasField);

        Button cancelButton = new Button("Cancel", e -> {
            dialog.close();
        });
        dialog.getFooter().add(cancelButton);

        Button useButton = new Button("Use", e -> {
            dialog.close();
            setReplicas(context, replicasField.getValue());
        });
        useButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        useButton.addClickShortcut(Key.ENTER);
        dialog.getFooter().add(useButton);

        dialog.addAttachListener(e -> replicasField.focus());
        dialog.open();

    }

    private void setReplicas(ExecutionContext context, Integer value) {
        final var progress = new ProgressDialog();
        progress.setMax(context.getSelected().size());
        progress.setHeaderTitle("Scale Deployment");
//        final var jsonPatchStr =
//                "[{\"op\":\"replace\",\"path\":\"/spec/replicas\",\"value\":" + value +"}]";

        context.getUi().access(() -> {
            progress.open();

            context.getSelected().forEach(obj -> {
                progress.next(obj.getMetadata().getName());
                try {

                    Kubectl.scale(V1Deployment.class).apiClient(context.getCluster().getApiProvider().getClient())
                            .namespace(obj.getMetadata().getNamespace())
                            .name(obj.getMetadata().getName())
                            .replicas(value)
                            .execute();
                } catch (Exception e) {
                    context.getErrors().add(e);
                }
            });
            progress.close();
            context.finished();
        });
    }

    @Override
    public String getTitle() {
        return "Scale";
    }

    @Override
    public String getMenuPath() {
        return ACTIONS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return 1000;
    }

    @Override
    public String getShortcutKey() {
        return "S";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public AbstractIcon getIcon() {
        return VaadinIcon.SCALE.create();
    }
}
