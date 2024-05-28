package de.mhus.kt2l.resources.secret;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Secret;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@WithRole(UsersConfiguration.ROLE.WRITE)
public class EditSecretAction implements ResourceAction {

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandleResourceType(Cluster cluster, K8s resourceType) {
        return K8s.SECRET.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, K8s resourceType, Set<? extends KubernetesObject> selected) {
        return canHandleResourceType(cluster, resourceType) && selected.size() == 1;
    }

    @Override
    public void execute(ExecutionContext context) {
        var selected = context.getSelected().iterator().next();
        openPanelTab(panelService, context.getSelectedTab(), context.getCore(), context.getCluster(), (V1Secret) selected);
    }

    public static DeskTab openPanelTab(PanelService panelService, DeskTab parentTab, Core core, Cluster cluster, V1Secret secret) {
        return panelService.addPanel(
                parentTab,
                "edit-secret-" + secret.getMetadata().getNamespace() + "-" + secret.getMetadata().getName(),
                "Edit " + secret.getMetadata().getName(),
                true,
                VaadinIcon.PASSWORD.create(),
                () -> new EditSecretPanel(core, cluster, secret)
        ).setHelpContext("edit_secret").select();
    }

    @Override
    public String getTitle() {
        return "Edit;icon=" + VaadinIcon.PASSWORD.name();
    }

    @Override
    public String getMenuPath() {
        return ACTIONS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return 2012;
    }

    @Override
    public String getShortcutKey() {
        return "E";
    }

    @Override
    public String getDescription() {
        return "Edit Secret content";
    }
}
