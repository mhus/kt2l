package de.mhus.kt2l.resources.pod;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.TextField;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.ProgressDialog;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.PatchUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@WithRole(UsersConfiguration.ROLE.WRITE)
public class ContainerResizeLimitsAction implements ResourceAction {
    @Override
    public boolean canHandleResourceType(Cluster cluster, K8s resourceType) {
        return K8s.POD.equals(resourceType) || K8s.CONTAINER.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, K8s resourceType, Set<? extends KubernetesObject> selected) {
        return canHandleResourceType(cluster, resourceType) && selected.size() > 0;
    }

    @Override
    public void execute(ExecutionContext context) {

        // find maximum limits

        Quantity cpuLimit = new Quantity("0");
        Quantity memLimit = new Quantity("0");
        for (KubernetesObject selected : context.getSelected()) {
            V1Container container = null;
            if (selected instanceof V1Pod pod) {
                var containers = pod.getSpec().getContainers();
                if (containers != null) {
                    container = containers.get(0);
                }
            } else if (selected instanceof ContainerResource c) {
                container = c.getContainer();
            }

            if (container != null) {
                if (container.getResources().getLimits() != null) {
                    var localCpuLimit = container.getResources().getLimits().get("cpu");
                    var localMemLimit = container.getResources().getLimits().get("memory");
                    if (cpuLimit == null || localCpuLimit.getNumber().doubleValue() > cpuLimit.getNumber().doubleValue()) {
                        cpuLimit = localCpuLimit;
                    }
                    if (memLimit == null || localMemLimit.getNumber().doubleValue() > memLimit.getNumber().doubleValue()) {
                        memLimit = localMemLimit;
                    }
                    }
            }
        }

        // show dialog

        final var cpuLimitFinal = cpuLimit.toSuffixedString();
        final var memLimitFinal = memLimit.toSuffixedString();

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Resize Limits");
        dialog.setWidth("400px");
        dialog.setHeight("400px");
        TextField cpuField = new TextField("CPU Limit");
        cpuField.setValue(cpuLimitFinal);
        cpuField.setWidthFull();
        dialog.add(cpuField);
        TextField memField = new TextField("Memory Limit");
        memField.setWidthFull();
        memField.setValue(memLimitFinal);
        dialog.add(memField);

        Button cancelButton = new Button("Cancel", e -> {
            dialog.close();
        });
        dialog.getFooter().add(cancelButton);

        Button useButton = new Button("Use", e -> {

            var cpu = cpuField.getValue().equals(cpuLimitFinal) ? null : cpuLimitFinal;
            var mem = memField.getValue().equals(memLimitFinal) ? null : memLimitFinal;

            dialog.close();
            setLimits(context, cpu, mem);
        });
        useButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        useButton.addClickShortcut(Key.ENTER);
        dialog.getFooter().add(useButton);

        dialog.open();

    }

    private void setLimits(ExecutionContext context, String cpu, String mem) {
        if (cpu == null && mem == null) return;

        final var progress = new ProgressDialog();
        progress.setMax(context.getSelected().size());
        progress.setHeaderTitle("Resize Limits");

        context.getUi().access(() -> {
            progress.open();
            for (KubernetesObject selected : context.getSelected()) {
                try {
                    V1Container selContainer = null;
                    V1Pod selPod = null;
                    if (selected instanceof V1Pod pod) {
                        var containers = pod.getSpec().getContainers();
                        if (containers != null) {
                            selContainer = containers.get(0);
                            selPod = pod;
                        }
                    } else if (selected instanceof ContainerResource c) {
                        selContainer = c.getContainer();
                        selPod = c.getPod();
                    }

                    if (selContainer != null) {
                        progress.next(selContainer.getName());
                        setLimits(context, selPod, selContainer, cpu, mem);
                    }

                } catch (Exception e) {
                    LOGGER.error("Error setting limits", e);
                    context.getErrors().add(e);
                }
            }
            progress.close();
        });
    }

    private void setLimits(ExecutionContext context, V1Pod pod, V1Container container, String cpu, String mem) throws ApiException {

        var jsonPatchStr = "{\"spec\":{\"containers\":[{\"name\":\"" + container.getName() + "\", \"resources\":{\"limits\":{";
        if (cpu != null)
            jsonPatchStr+="\"cpu\":\"" + cpu + "\"";
        if (cpu != null && mem != null)
            jsonPatchStr+=",";
        if (mem != null)
            jsonPatchStr+="\"memory\":\"" + mem + "\"";
        jsonPatchStr+="}}}]}}";

        final var finalJsonPatchStr = jsonPatchStr;

        PatchUtils.patch(
                V1Pod.class,
                () -> context.getCluster().getApiProvider().getCoreV1Api().patchNamespacedPodCall(
                        pod.getMetadata().getName(),
                        pod.getMetadata().getNamespace(),
                        new V1Patch(finalJsonPatchStr),
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

    }

    @Override
    public String getTitle() {
        return "Resize Limits";
    }

    @Override
    public String getMenuPath() {
        return ACTIONS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return 1050;
    }

    @Override
    public String getShortcutKey() {
        return "I";
    }

    @Override
    public String getDescription() {
        return "";
    }
}
