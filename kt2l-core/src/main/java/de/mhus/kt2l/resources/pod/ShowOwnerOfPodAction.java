package de.mhus.kt2l.resources.pod;

import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@WithRole(UsersConfiguration.ROLE.READ)
public class ShowOwnerOfPodAction implements ResourceAction {

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandleResourceType(Cluster cluster, K8s resourceType) {
        return K8s.POD.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, K8s resourceType, Set<? extends KubernetesObject> selected) {
        if (!canHandleResourceType(cluster, resourceType) || selected.size() != 1) return false;
        V1Pod pod = (V1Pod)selected.iterator().next();
        var ownerReference = pod.getMetadata().getOwnerReferences();
        if (ownerReference == null || ownerReference.isEmpty()) return false;
        var kind = ownerReference.get(0).getKind();
        return kind.equals("ReplicaSet") || kind.equals("Deployment");
    }

    @Override
    public void execute(ExecutionContext context) {
        if (!canHandleResource(context.getCluster(), context.getResourceType(), context.getSelected())) return;
        V1Pod pod = (V1Pod)context.getSelected().iterator().next();
        var ownerReference = pod.getMetadata().getOwnerReferences();
        var kind = ownerReference.get(0).getKind();

        switch (kind) {
            case "ReplicaSet":
//                panelService.showReplicaSet(context.getCluster(), ownerReference.get(0).getName());
                break;
            case "Deployment":
//                panelService.showDeployment(context.getCluster(), ownerReference.get(0).getName());
                break;
        }
    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    public String getMenuPath() {
        return "";
    }

    @Override
    public int getMenuOrder() {
        return 0;
    }

    @Override
    public String getShortcutKey() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }
}
