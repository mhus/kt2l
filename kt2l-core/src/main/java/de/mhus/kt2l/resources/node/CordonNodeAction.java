package de.mhus.kt2l.resources.node;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.NumberField;
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

@Component
@WithRole(UsersConfiguration.ROLE.ADMIN)
public class CordonNodeAction implements ResourceAction {
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
        dialog.setHeader("Cordon Node");

        var form = new FormLayout();
        var text = new Div("Do you really want to cordon the selected " + context.getSelected().size() + " node(s)?");

        form.add(text);
        form.setResponsiveSteps(
                // Use one column by default
                new FormLayout.ResponsiveStep("0", 1),
                // Use two columns, if layout's width exceeds 500px
                new FormLayout.ResponsiveStep("500px", 2));
        form.setWidthFull();
        form.setColspan(text, 2);
        dialog.setWidth("80%");
        dialog.setConfirmText("Cordon");
        dialog.setCancelText("Cancel");
        dialog.setCloseOnEsc(false);
        dialog.setCancelable(true);
        dialog.setConfirmButtonTheme("error primary");
        dialog.setCancelButtonTheme("tertiary");
        dialog.addConfirmListener(
                e -> {
                    drainNodes(context);
                });

        dialog.setText(form);
        dialog.open();

    }

    private void drainNodes(ExecutionContext context) {

        ProgressDialog progress = new ProgressDialog();
        progress.setHeaderTitle("Cordon Nodes");
        progress.setMax(context.getSelected().size());
        progress.open();

        Thread.startVirtualThread(() -> {
            for (KubernetesObject obj : context.getSelected()) {
                context.getUi().access(() -> progress.next(obj.getMetadata().getName()));
                var cordon = Kubectl.cordon();
                try {
                    cordon.apiClient(context.getCluster().getApiProvider().getClient())
                            .name(obj.getMetadata().getName()).execute();
                    context.getUi().access(() -> UiUtil.showSuccessNotification("Node " + obj.getMetadata().getName() + " cordoned"));
                } catch (Exception e) {
                    context.getUi().access(() -> UiUtil.showErrorNotification("Error cordon node " + obj.getMetadata().getName(), e));
                }
            }
            context.getUi().access(() -> progress.close());
        });
    }

    @Override
    public String getTitle() {
        return "Cordon Node";
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
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public AbstractIcon getIcon() {
        return VaadinIcon.CLOSE.create();
    }
}
