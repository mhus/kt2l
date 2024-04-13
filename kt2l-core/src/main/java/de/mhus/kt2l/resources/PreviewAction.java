package de.mhus.kt2l.resources;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
@WithRole(UsersConfiguration.ROLE.READ)
public class PreviewAction implements ResourceAction {
    @Override
    public boolean canHandleResourceType(String resourceType) {
        return true;
    }

    @Override
    public boolean canHandleResource(String resourceType, Set<? extends KubernetesObject> selected) {
        return selected.size() > 0;
    }

    @Override
    public void execute(ExecutionContext context) {
        // prepare preview
        StringBuilder sb = new StringBuilder();
        context.getSelected().forEach(
                res -> {
                    var kind = res.getKind() == null ? context.getResourceType() : res.getKind();
                    sb.append(">>> ").append(kind).append(" ").append(res.getMetadata().getName()).append('\n');
                    sb.append(K8sUtil.toYaml(res)).append('\n');
                }
        );

        Dialog dialog = new Dialog();
        dialog.getHeader().add(VaadinIcon.FILE_PRESENTATION.create());
        dialog.setHeaderTitle("Preview");
        var preview = new TextArea();
        preview.setReadOnly(true);
        preview.setSizeFull();
        preview.setValue(sb.toString());
        dialog.add(preview);
        dialog.setWidth("80%");
        dialog.setHeight("80%");
        final var ui = context.getUi();
        Button closeButton = new Button("Close", e -> {
            dialog.close();
            ui.remove(dialog);
        });
        dialog.getFooter().add(closeButton);
        dialog.open();
    }

    @Override
    public String getTitle() {
        return "Preview;icon=" + VaadinIcon.FILE_PRESENTATION;
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.VIEW_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.VIEW_ORDER+100;
    }

    @Override
    public String getShortcutKey() {
        return "Z";
    }

    @Override
    public String getDescription() {
        return "Preview Description";
    }
}
