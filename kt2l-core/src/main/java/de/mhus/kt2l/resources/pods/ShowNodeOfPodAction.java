package de.mhus.kt2l.resources.pods;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.config.UsersConfiguration.ROLE;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.resources.ResourcesFilter;
import de.mhus.kt2l.resources.ResourcesGridPanel;
import de.mhus.kt2l.core.WithRole;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@WithRole(ROLE.READ)
public class ShowNodeOfPodAction implements ResourceAction {
    @Override
    public boolean canHandleResourceType(String resourceType) {
        return K8sUtil.RESOURCE_PODS.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(String resourceType, Set<? extends KubernetesObject> selected) {
        return canHandleResourceType(resourceType) && selected.size() == 1;
    }

    @Override
    public void execute(ExecutionContext context) {

        var pod = (V1Pod)context.getSelected().iterator().next();
        final var nodeName = pod.getSpec().getNodeName();
        final var podName = pod.getMetadata().getName();
        ((ResourcesGridPanel)context.getSelectedTab().getPanel()).showResources(K8sUtil.RESOURCE_NODES, new ResourcesFilter() {
            @Override
            public boolean filter(KubernetesObject res) {
                if (res instanceof io.kubernetes.client.openapi.models.V1Node node) {
                    return node.getMetadata().getName().equals(nodeName);
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "Node for pod " + podName;
            }
        });
    }

    @Override
    public String getTitle() {
        return "Nodes;icon=" + VaadinIcon.OPEN_BOOK;
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.VIEW_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.VIEW_ORDER + 100;
    }

    @Override
    public String getShortcutKey() {
        return "N";
    }

    @Override
    public String getDescription() {
        return "Show node for the pod";
    }
}
