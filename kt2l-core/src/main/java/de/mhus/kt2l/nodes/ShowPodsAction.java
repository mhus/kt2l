package de.mhus.kt2l.nodes;

import de.mhus.kt2l.generic.ExecutionContext;
import de.mhus.kt2l.generic.ResourceAction;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.ResourcesFilter;
import de.mhus.kt2l.resources.ResourcesGridPanel;
import io.kubernetes.client.common.KubernetesObject;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ShowPodsAction implements ResourceAction {
    @Override
    public boolean canHandleResourceType(String resourceType) {
        return K8sUtil.RESOURCE_NODES.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(String resourceType, Set<? extends KubernetesObject> selected) {
        return canHandleResourceType(resourceType) && selected.size() == 1;
    }

    @Override
    public void execute(ExecutionContext context) {

        final String nodeName = context.getSelected().iterator().next().getMetadata().getName();
        ((ResourcesGridPanel)context.getSelectedTab().getPanel()).showResources(K8sUtil.RESOURCE_PODS, new ResourcesFilter() {
            @Override
            public boolean filter(KubernetesObject res) {
                if (res instanceof io.kubernetes.client.openapi.models.V1Pod pod) {
                    return pod.getSpec().getNodeName().equals(nodeName);
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "Pods on node " + nodeName;
            }
        });
    }

    @Override
    public String getTitle() {
        return "Pods";
    }

    @Override
    public String getMenuBarPath() {
        return "";
    }

    @Override
    public String getShortcutKey() {
        return "p";
    }

    @Override
    public String getPopupPath() {
        return "";
    }

    @Override
    public String getDescription() {
        return "Show pods of the selected node";
    }
}
