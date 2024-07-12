package de.mhus.kt2l.resources.common;


import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.ui.ProgressDialog;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@WithRole(UsersConfiguration.ROLE.WRITE)
@Slf4j
public class AddLabelAction implements ResourceAction {
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
        var text = new Div("Add Label to the selected "+context.getSelected().size()+" resource(s)");
        var key = new TextField("Key");
        var value = new TextField("Value");
        form.add(text, key, value);
        form.setResponsiveSteps(
                // Use one column by default
                new FormLayout.ResponsiveStep("0", 1),
                // Use two columns, if layout's width exceeds 500px
                new FormLayout.ResponsiveStep("500px", 2));
        form.setWidthFull();
        form.setColspan(text, 2);

        dialog.setText(form);
        dialog.setWidth("80%");
        dialog.setConfirmText("Add Label");
        dialog.setCancelText("Cancel");
        dialog.setCancelable(true);
        dialog.setCloseOnEsc(true);
        dialog.setConfirmButtonTheme("primary");
        dialog.setCancelButtonTheme("tertiary");
        dialog.addConfirmListener(e -> addLabel(context, key.getValue(), value.getValue()));
        dialog.addAttachListener(event -> key.focus());
        dialog.open();
    }

    private void addLabel(ExecutionContext context, String key, String value) {
        ProgressDialog dialog = new ProgressDialog();
        dialog.setHeaderTitle("Add Label");
        dialog.setMax(context.getSelected().size());
        dialog.open();
        Thread.startVirtualThread(() -> {
            for (KubernetesObject obj : context.getSelected()) {
                context.getUi().access(() -> dialog.next(obj.getMetadata().getName()));
                try {
                    var label = Kubectl.label(obj.getClass())
                            .apiClient(context.getCluster().getApiProvider().getClient())
                            .name(obj.getMetadata().getName());
                    if (obj.getMetadata().getNamespace() != null)
                        label.namespace(obj.getMetadata().getNamespace());
                    label.addLabel(K8sUtil.normalizeLabelKey(key), K8sUtil.normalizeLabelValue(value)).execute();
                    context.getUi().access(() -> UiUtil.showSuccessNotification("Label added to " + obj.getMetadata().getName()));
                } catch (Exception e) {
                    context.getUi().access(() -> UiUtil.showErrorNotification("Error add label to " + obj.getMetadata().getName(), e));
                }
            }
            context.getUi().access(dialog::close);
        });
    }

    @Override
    public String getTitle() {
        return "Add Label;icon=" + VaadinIcon.FILE_ADD;
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.TOOLS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.TOOLS_ORDER + 200;
    }

    @Override
    public String getShortcutKey() {
        return "";
    }

    @Override
    public String getDescription() {
        return "Add a label to the selected resources";
    }
}
