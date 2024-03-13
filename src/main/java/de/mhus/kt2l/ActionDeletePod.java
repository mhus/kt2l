package de.mhus.kt2l;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import de.mhus.commons.errors.UsageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class ActionDeletePod implements XUiAction {

    @Override
    public boolean canHandleResourceType(String resourceType) {
        return K8sUtil.RESOURCE_PODS.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(String resourceType, Set<? extends Object> selected) {
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
        return "Delete";
    }

    @Override
    public String getMenuBarPath() {
        return "";
    }

    @Override
    public String getShortcutKey() {
        return "";
    }

    @Override
    public String getPopupPath() {
        return "";
    }
}
