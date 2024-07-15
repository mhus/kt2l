package de.mhus.kt2l.resources.common;


import com.vaadin.flow.component.combobox.ComboBox;
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
import java.util.TreeSet;

@Component
@WithRole(UsersConfiguration.ROLE.WRITE)
@Slf4j
public class RemoveAnnotationAction implements ResourceAction {

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
        // collect all annotations
        Set<String> existing = new TreeSet<>();
        for (KubernetesObject obj : context.getSelected()) {
            if (obj.getMetadata().getAnnotations() != null)
                existing.addAll(obj.getMetadata().getAnnotations().keySet());
        }
        // dialog
        ConfirmDialog dialog = new ConfirmDialog();
        var form = new FormLayout();
        var text = new Div("Remove Annotation from the selected "+context.getSelected().size()+" resource(s)");
        var key = new ComboBox<String>("Key");
        key.setItems(existing);
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
        dialog.setConfirmText("Remove Annotation");
        dialog.setCancelText("Cancel");
        dialog.setCancelable(true);
        dialog.setCloseOnEsc(true);
        dialog.setConfirmButtonTheme("primary");
        dialog.setCancelButtonTheme("tertiary");
        dialog.addConfirmListener(e -> removeAnnotation(context, key.getValue()));
        dialog.addAttachListener(event -> key.focus());
        dialog.open();
    }

    private void removeAnnotation(ExecutionContext context, String key) {
        ProgressDialog dialog = new ProgressDialog();
        dialog.setHeaderTitle("Remove Annotation");
        dialog.setMax(context.getSelected().size());
        dialog.open();
        Thread.startVirtualThread(() -> {
            for (KubernetesObject obj : context.getSelected()) {
                context.getUi().access(() -> dialog.next(obj.getMetadata().getName()));
                try {
                    var patch = "[{\"op\":\"remove\",\"path\":\"/metadata/annotations/" + K8sUtil.normalizeAnnotationKey(key) + "\"}]";
                    var handler = k8sService.getTypeHandler(obj, context.getCluster(), context.getType());
                    handler.patch(context.getCluster().getApiProvider(), obj, patch);
                    context.getUi().access(() -> UiUtil.showSuccessNotification("Annotation removed from " + obj.getMetadata().getName()));
                } catch (Exception e) {
                    context.getUi().access(() -> UiUtil.showErrorNotification("Error remove annotation from " + obj.getMetadata().getName(), e));
                }
            }
            context.getUi().access(dialog::close);
        });
    }

    @Override
    public String getTitle() {
        return "Remove Annotation";
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.EDIT_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.EDIT_ORDER + 211;
    }

    @Override
    public String getShortcutKey() {
        return "";
    }

    @Override
    public String getDescription() {
        return "Remove a annotation from the selected resources";
    }

    @Override
    public AbstractIcon getIcon() {
        return VaadinIcon.FILE_REMOVE.create();
    }
}
