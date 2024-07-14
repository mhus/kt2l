package de.mhus.kt2l.resources.common;

import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.resources.ResourceFormFactory;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@WithRole(UsersConfiguration.ROLE.WRITE)
@Slf4j
public class ResourceEditFormAction implements ResourceAction {

    @Autowired
    private List<ResourceFormFactory> formFactories;
    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return getFactory(cluster, type) != null;
    }

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {
        return canHandleType(cluster, type) && selected.size() == 1 && getFactory(cluster, type).canHandleResource(cluster, type, selected.iterator().next());
    }

    protected ResourceFormFactory getFactory(Cluster cluster, V1APIResource type) {
        return formFactories.stream()
                .filter(f -> f.canHandleType(cluster, type))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void execute(ExecutionContext context) {
        var factory = getFactory(context.getCluster(), context.getType());
        if (factory == null)
            throw new IllegalStateException("No factory found for " + context.getType().getKind());
        var form = factory.createForm(context);
        panelService.showEditFormPanel(context.getSelectedTab(), form, context.getCluster(), context.getSelected().iterator().next(), context.getType()).select();
    }

    @Override
    public String getTitle() {
        return "Edit Form";
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.EDIT_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.EDIT_ORDER + 101;
    }

    @Override
    public String getShortcutKey() {
        return "Ctrl+F";
    }

    @Override
    public String getDescription() {
        return "Edit the selected resource with a form";
    }

    @Override
    public AbstractIcon getIcon() {
        return VaadinIcon.EDIT.create();
    }
}
