package de.mhus.kt2l.resources.role;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.resources.ResourcesFilter;
import de.mhus.kt2l.resources.ResourcesGridPanel;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Role;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@WithRole(UsersConfiguration.ROLE.READ)
public class ShowRoleBindingsForRoleAction implements ResourceAction {

    @Override
    public boolean canHandleResourceType(Cluster cluster, K8s resourceType) {
        return K8s.ROLE.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, K8s resourceType, Set<? extends KubernetesObject> selected) {
        return canHandleResourceType(cluster, resourceType) && selected.size() == 1;
    }

    @Override
    public void execute(ExecutionContext context) {

        var source = context.getSelected().iterator().next();
        final var name = source.getMetadata().getName();
        final var namespace = source.getMetadata().getNamespace();

        ((ResourcesGridPanel)context.getSelectedTab().getPanel()).showResources(K8s.ROLE_BINDING, namespace, new ResourcesFilter() {
            @Override
            public boolean filter(KubernetesObject res) {
                if (res instanceof io.kubernetes.client.openapi.models.V1RoleBinding roleBinding) {
                    return roleBinding.getRoleRef().getName().equals(name);
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "RoleBindings for Role " + name;
            }
        }, null);

    }

    @Override
    public String getTitle() {
        return "RoleBindings;icon=" + VaadinIcon.ARROW_FORWARD;
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.VIEW_PATH;
    }

    @Override
    public int getMenuOrder() {
        return 1234;
    }

    @Override
    public String getShortcutKey() {
        return "CTRL+B";
    }

    @Override
    public String getDescription() {
        return "Show RoleBindings for Role";
    }
}
