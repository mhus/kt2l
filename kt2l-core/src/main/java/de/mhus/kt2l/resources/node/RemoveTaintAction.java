package de.mhus.kt2l.resources.node;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.ui.ProgressDialog;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.openapi.models.V1APIResource;
import org.springframework.stereotype.Component;

import java.util.Set;

import static de.mhus.commons.tools.MString.isEmpty;

@Component
@WithRole(UsersConfiguration.ROLE.ADMIN)
public class RemoveTaintAction implements ResourceAction  {
    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return K8s.NODE.equals(type);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {
        return canHandleType(cluster, type) && selected.size() > 0;
    }

    @Override
    public void execute(ExecutionContext context) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Remove Taint");
        var form = new FormLayout();
        var text = new Div("Remove a taint from the selected " + context.getSelected().size() + " node(s)");
        var key = new TextField("Key");
        var effect = new TextField("Effect");
        form.add(text, key, effect);
        form.setResponsiveSteps(
                // Use one column by default
                new FormLayout.ResponsiveStep("0", 1),
                // Use two columns, if layout's width exceeds 500px
                new FormLayout.ResponsiveStep("500px", 2));
        form.setWidthFull();
        form.setColspan(text, 2);
        dialog.setWidth("80%");
        dialog.setConfirmText("Add Taint");
        dialog.setCancelText("Cancel");
        dialog.setCloseOnEsc(false);
        dialog.setCancelable(true);
        dialog.setConfirmButtonTheme("primary");
        dialog.setCancelButtonTheme("tertiary");
        dialog.addConfirmListener(
                e -> addTaint(context, key.getValue(), effect.getValue()));
        dialog.addAttachListener(event -> key.focus());
        dialog.add(form);
        dialog.open();
    }

    private void addTaint(ExecutionContext context, String key, String effect) {
        ProgressDialog dialog = new ProgressDialog();
        dialog.setHeaderTitle("Add Taint");
        dialog.setMax(context.getSelected().size());
        dialog.open();
        Thread.startVirtualThread(() -> {
            for (KubernetesObject obj : context.getSelected()) {
                context.getUi().access(() -> dialog.next(obj.getMetadata().getName()));
                try {
                    var taint = Kubectl.taint().apiClient(context.getCluster().getApiProvider().getClient()).name(obj.getMetadata().getName());
                    if (isEmpty(effect)) {
                        taint.removeTaint(key).execute();
                    } else {
                        taint.removeTaint(key, effect).execute();
                    }
                    context.getUi().access(() -> UiUtil.showSuccessNotification("Taint removed from node " + obj.getMetadata().getName()));
                } catch (Exception e) {
                    context.getUi().access(() -> UiUtil.showErrorNotification("Error removing taint from node " + obj.getMetadata().getName(), e));
                }
            }
        });
    }

    @Override
    public String getTitle() {
        return "Remove Taint;icon=" + VaadinIcon.FILE_REMOVE;
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.TOOLS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.TOOLS_ORDER + 111;
    }

    @Override
    public String getShortcutKey() {
        return "";
    }

    @Override
    public String getDescription() {
        return "Remove a taint from the selected nodes";
    }
}
