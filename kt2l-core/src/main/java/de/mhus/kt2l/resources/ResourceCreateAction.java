package de.mhus.kt2l.resources;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8s;
import io.kubernetes.client.common.KubernetesObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@WithRole(UsersConfiguration.ROLE.WRITE)
@Slf4j
public class ResourceCreateAction implements ResourceAction {

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandleResourceType(K8s.RESOURCE resourceType) {
        return true;
    }

    @Override
    public boolean canHandleResource(K8s.RESOURCE resourceType, Set<? extends KubernetesObject> selected) {
        return true;
    }

    @Override
    public void execute(ExecutionContext context) {
        panelService.addPanel(
                context.getSelectedTab(),
                context.getCluster().getName() + ":" + context.getNamespace() + ":create",
                context.getNamespace(),
                false,
                VaadinIcon.FILE_ADD.create(),
                () ->
                        new ResourceCreatePanel(
                                context.getCluster(),
                                context.getCore(),
                                context.getNamespace()
                        )).setHelpContext("create").select();

    }

    @Override
    public String getTitle() {
        return "Create;icon=" + VaadinIcon.FILE_ADD;
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.ACTIONS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.ACTIONS_ORDER + 90;
    }

    @Override
    public String getShortcutKey() {
        return "CONTROL+C";
    }

    @Override
    public String getDescription() {
        return "Create new resources";
    }
}
