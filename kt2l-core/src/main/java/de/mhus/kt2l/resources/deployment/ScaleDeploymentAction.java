package de.mhus.kt2l.resources.deployment;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.IntegerField;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.ProgressDialog;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.util.PatchUtils;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@WithRole(UsersConfiguration.ROLE.WRITE)
public class ScaleDeploymentAction implements ResourceAction {
    @Override
    public boolean canHandleResourceType(K8s resourceType) {
        return K8s.DEPLOYMENT.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(K8s resourceType, Set<? extends KubernetesObject> selected) {
        return canHandleResourceType(resourceType) && selected.size() > 0;
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

        dialog.open();

    }

    private void setReplicas(ExecutionContext context, Integer value) {
        final var progress = new ProgressDialog();
        progress.setMax(context.getSelected().size());
        progress.setHeaderTitle("Scale Deployment");
        final var jsonPatchStr =
                "[{\"op\":\"replace\",\"path\":\"/spec/replicas\",\"value\":" + value +"}]";

        context.getUi().access(() -> {
            progress.open();

            context.getSelected().forEach(obj -> {
                progress.setProgress(progress.getProgress() + 1, obj.getMetadata().getName());
                try {
                    PatchUtils.patch(
                            V1Deployment.class,
                            () -> context.getCluster().getApiProvider().getAppsV1Api().patchNamespacedDeploymentCall(
                                    obj.getMetadata().getName(),
                                    obj.getMetadata().getNamespace(),
                                    new V1Patch(jsonPatchStr),
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null
                            ),
                            V1Patch.PATCH_FORMAT_JSON_PATCH,
                            context.getCluster().getApiProvider().getClient()
                    );

                } catch (Exception e) {
                    context.getErrors().add(e);
                }
            });
            progress.close();
        });
    }

    @Override
    public String getTitle() {
        return "Scale;icon=" + VaadinIcon.SCALE;
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
}
