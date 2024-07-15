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
public class DrainNodeAction implements ResourceAction {
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
        dialog.setHeader("Drain Node");

        var form = new FormLayout();
        var text = new Div("Do you really want to drain the selected " + context.getSelected().size() + " node(s)?");

        var forceChk = new Checkbox("Force Drain");
        var ignoreDaemonSetsChk = new Checkbox("Ignore DaemonSets");
        ignoreDaemonSetsChk.setValue(true);
        var gracePeriodCnt = new NumberField("Grace Period (sec)");
        gracePeriodCnt.setMin(-1);
        gracePeriodCnt.setMax(1000);
        gracePeriodCnt.setValue(-1.0);
        gracePeriodCnt.setStep(1);
        form.add(text, forceChk, ignoreDaemonSetsChk, gracePeriodCnt);
        form.setResponsiveSteps(
                // Use one column by default
                new FormLayout.ResponsiveStep("0", 1),
                // Use two columns, if layout's width exceeds 500px
                new FormLayout.ResponsiveStep("500px", 2));
        form.setWidthFull();
        form.setColspan(text, 2);
        dialog.setWidth("80%");
        dialog.setConfirmText("Drain");
        dialog.setCancelText("Cancel");
        dialog.setCloseOnEsc(false);
        dialog.setCancelable(true);
        dialog.setConfirmButtonTheme("error primary");
        dialog.setCancelButtonTheme("tertiary");
        dialog.addConfirmListener(
                e -> {
                    drainNodes(context, forceChk.getValue(), ignoreDaemonSetsChk.getValue(), gracePeriodCnt.getValue().intValue());
                });

        dialog.setText(form);
        dialog.open();

    }

    private void drainNodes(ExecutionContext context, boolean force, boolean ignoreDaemonSets, int gracePeriod) {

        ProgressDialog progress = new ProgressDialog();
        progress.setHeaderTitle("Drain Nodes");
        progress.setMax(context.getSelected().size());
        progress.open();

        Thread.startVirtualThread(() -> {
            for (KubernetesObject obj : context.getSelected()) {
                context.getUi().access(() -> progress.next(obj.getMetadata().getName()));
                var drain = Kubectl.drain();
                if (force)
                    drain.force();
                if (ignoreDaemonSets)
                    drain.ignoreDaemonSets();
                if (gracePeriod >= 0)
                    drain.gracePeriod(gracePeriod);

                try {
                    drain.apiClient(context.getCluster().getApiProvider().getClient())
                            .name(obj.getMetadata().getName()).execute();
                    context.getUi().access(() -> UiUtil.showSuccessNotification("Node " + obj.getMetadata().getName() + " drained"));
                } catch (Exception e) {
                    context.getUi().access(() -> UiUtil.showErrorNotification("Error draining node " + obj.getMetadata().getName(), e));
                }
            }
            context.getUi().access(() -> progress.close());
        });
    }

    @Override
    public String getTitle() {
        return "Drain Node";
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.ACTIONS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.ACTIONS_ORDER + 110;
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
        return VaadinIcon.CLOSE_CIRCLE_O.create();
    }
}
