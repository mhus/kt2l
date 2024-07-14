package de.mhus.kt2l.resources.common;


import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.ui.ProgressDialog;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@WithRole(UsersConfiguration.ROLE.WRITE)
@Slf4j
public class RemoveLabelAction implements ResourceAction {

    @Autowired
    private K8sService k8sService;

    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return !K8s.CONTAINER.equals(type);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {
        return canHandleType(cluster, type) && selected.size() > 0;
    }

    @Override
    public void execute(ExecutionContext context) {
        ConfirmDialog dialog = new ConfirmDialog();
        var form = new FormLayout();
        var text = new Div("Remove Label from the selected "+context.getSelected().size()+" resource(s)");
        var key = new TextField("Key");
        form.add(text, key);
        form.setResponsiveSteps(
                // Use one column by default
                new FormLayout.ResponsiveStep("0", 1),
                // Use two columns, if layout's width exceeds 500px
                new FormLayout.ResponsiveStep("500px", 2));
        form.setWidthFull();
        form.setColspan(text, 2);

        dialog.setText(form);
        dialog.setWidth("80%");
        dialog.setConfirmText("Remove Label");
        dialog.setCancelText("Cancel");
        dialog.setCancelable(true);
        dialog.setCloseOnEsc(true);
        dialog.setConfirmButtonTheme("primary");
        dialog.setCancelButtonTheme("tertiary");
        dialog.addConfirmListener(e -> removeLabel(context, key.getValue()));
        dialog.addAttachListener(event -> key.focus());
        dialog.open();
    }

    private void removeLabel(ExecutionContext context, String key) {
        ProgressDialog dialog = new ProgressDialog();
        dialog.setHeaderTitle("Remove Label");
        dialog.setMax(context.getSelected().size());
        dialog.open();
        Thread.startVirtualThread(() -> {
            for (KubernetesObject obj : context.getSelected()) {
                context.getUi().access(() -> dialog.next(obj.getMetadata().getName()));
                try {
                    var patch = "[{\"op\":\"remove\",\"path\":\"/metadata/labels/" + K8sUtil.normalizeLabelKey(key) + "\"}]";
                    var handler = k8sService.getTypeHandler(obj, context.getCluster(), context.getType());
                    handler.patch(context.getCluster().getApiProvider(), obj, patch);
                    context.getUi().access(() -> UiUtil.showSuccessNotification("Label removed from " + obj.getMetadata().getName()));
                } catch (Exception e) {
                    context.getUi().access(() -> UiUtil.showErrorNotification("Error remove label from " + obj.getMetadata().getName(), e));
                }
            }
            context.getUi().access(dialog::close);
        });
    }

    @Override
    public String getTitle() {
        return "Remove Label";
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.EDIT_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.EDIT_ORDER + 201;
    }

    @Override
    public String getShortcutKey() {
        return "";
    }

    @Override
    public String getDescription() {
        return "Remove a label from the selected resources";
    }

    @Override
    public AbstractIcon getIcon() {
        return VaadinIcon.FILE_REMOVE.create();
    }
}
