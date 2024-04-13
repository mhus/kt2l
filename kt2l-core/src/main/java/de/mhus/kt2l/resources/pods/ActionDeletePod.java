package de.mhus.kt2l.resources.pods;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.config.UsersConfiguration.ROLE;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.core.WithRole;
import io.kubernetes.client.common.KubernetesObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
@WithRole(ROLE.WRITE)
public class ActionDeletePod implements ResourceAction {

    @Override
    public boolean canHandleResourceType(String resourceType) {
        return K8sUtil.RESOURCE_PODS.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(String resourceType, Set<? extends KubernetesObject> selected) {
        return canHandleResourceType(resourceType) && selected.size() > 0;
    }

    @Override
    public void execute(ExecutionContext context) {

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete " + (context.getSelected().size() > 1 ? context.getSelected().size() + " Items" : context.getSelected() ) + "?");
        dialog.setText(
                "Are you sure you want to permanently delete this item?");

        dialog.setCancelable(true);
        dialog.addCancelListener(event -> {});

        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            deleteItems(context);
        });

        dialog.open();

    }

    private void deleteItems(ExecutionContext context) {
        context.setNeedGridRefresh(true);
        LOGGER.info("Delete pod");
        context.getSelected().forEach(o -> {
            if (o instanceof PodGrid.Pod) {
                PodGrid.Pod pod = (PodGrid.Pod) o;
                try {
                    context.getApi().deleteNamespacedPod(pod.getName(), pod.getNamespace(), null, null, null, null, null, null);
                } catch (Exception e) {
                    LOGGER.error("delete pod", e);
                    context.getErrors().add(e);
                }
            }
        });
        context.finished();
    }

    @Override
    public String getTitle() {
        return "Delete;icon=" + VaadinIcon.FILE_REMOVE;
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
        return null;
    }

    @Override
    public String getDescription() {
        return "Delete pods or container";
    }
}
