package de.mhus.kt2l.resources.configmap;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@WithRole(UsersConfiguration.ROLE.WRITE)
public class EditConfigMapAction implements ResourceAction {

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandleResourceType(Cluster cluster, K8s resourceType) {
        return K8s.CONFIG_MAP.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, K8s resourceType, Set<? extends KubernetesObject> selected) {
        return canHandleResourceType(cluster, resourceType) && selected.size() == 1;
    }

    @Override
    public void execute(ExecutionContext context) {
        var selected = context.getSelected().iterator().next();
        panelService.addPanel(
                context.getSelectedTab(),
                "edit-configmap-" + selected.getMetadata().getNamespace() + "-" + selected.getMetadata().getName(),
                "Edit " + selected.getMetadata().getName(),
                true,
                VaadinIcon.INPUT.create(),
                () -> new EditConfigMapPanel(context.getCore(), context.getCluster(), (V1ConfigMap) selected)
                ).setHelpContext("edit_cm").select();
    }

    @Override
    public String getTitle() {
        return "Edit;icon=" + VaadinIcon.INPUT.name();
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
        return "Edit ConfigMap content";
    }
}
