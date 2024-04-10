package de.mhus.kt2l.resources;

import de.mhus.kt2l.config.UsersConfiguration.ROLE;
import de.mhus.kt2l.ui.PanelService;
import de.mhus.kt2l.ui.WithRole;
import io.kubernetes.client.common.KubernetesObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@WithRole(ROLE.READ)
public class ActionDetails implements ResourceAction {

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandleResourceType(String resourceType) {
        return true;
    }

    @Override
    public boolean canHandleResource(String resourceType, Set<? extends KubernetesObject> selected) {
        return selected.size() == 1;
    }

    @Override
    public void execute(ExecutionContext context) {

        var selected = context.getSelected().iterator().next();

        var metadata = ((KubernetesObject) selected).getMetadata();
        var namespace = metadata.getNamespace();
        var name = metadata.getName();

        panelService.addDetailsPanel(context.getSelectedTab(), context.getClusterConfiguration(), context.getApi(), context.getResourceType(), selected).select();

    }

    @Override
    public String getTitle() {
        return "Details";
    }

    @Override
    public String getMenuBarPath() {
        return null;
    }

    @Override
    public String getShortcutKey() {
        return "d";
    }

    @Override
    public String getPopupPath() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Resource Details";
    }
}
