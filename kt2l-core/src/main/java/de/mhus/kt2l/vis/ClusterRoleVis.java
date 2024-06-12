package de.mhus.kt2l.vis;


import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.clusterrole.ClusterRoleWatch;
import de.mhus.kt2l.resources.role.RoleWatch;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ClusterRoleBinding;
import io.kubernetes.client.openapi.models.V1RoleBinding;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.vaadin.addons.visjs.network.main.Edge;
import org.vaadin.addons.visjs.network.main.Node;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ClusterRoleVis extends AbstractVisHandler {
    @Override
    public K8s[] getConnectedResourceTypes() {
        return new K8s[] {K8s.CLUSTER_ROLE_BINDING};
    }

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return ClusterRoleWatch.class;
    }

    @Override
    public void prepareNode(Node node, KubernetesObject res) {
        node.setColor("#baa");
        node.setMass(1);
    }

    @Override
    public K8s getManagedResourceType() {
        return K8s.CLUSTER_ROLE;
    }

    @Override
    public void createEdge(Edge edge, VisPanel.NodeStore v1, VisPanel.NodeStore v2) {

    }

    protected void updateConnectedEdge(String k1, VisPanel.NodeStore v1, String k2, VisPanel.NodeStore v2) {
        if (v1.k8sObject().getMetadata().getName() == null || v2.k8sObject().getMetadata().getName() == null) return;

        var roleBinding = (V1ClusterRoleBinding)v2.k8sObject();
        var role = v1.k8sObject().getMetadata().getName();

        if (roleBinding.getRoleRef().getName().equals(role)) {
            panel.processEdge(v1, v2);
        }

    }
}
