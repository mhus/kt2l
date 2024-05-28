package de.mhus.kt2l.vis;

import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.namespace.NamespaceWatch;
import io.kubernetes.client.common.KubernetesObject;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.vaadin.addons.visjs.network.main.Edge;
import org.vaadin.addons.visjs.network.main.Node;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NamespaceVis extends AbstractVisHandler {
    @Override
    public K8s[] getConnectedResourceTypes() {
        return new K8s[] {K8s.POD, K8s.DEPLOYMENT, K8s.DAEMON_SET, K8s.REPLICA_SET};
    }

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return NamespaceWatch.class;
    }

    @Override
    public void prepareNode(Node node, KubernetesObject res) {
        node.setColor("#f4f4f4");
        node.setBorderWidth(0);
        node.setMass(1);
        node.setGroup(res.getMetadata().getName());
    }

    @Override
    public K8s getManagedResourceType() {
        return K8s.NAMESPACE;
    }

    @Override
    public void createEdge(Edge edge, VisPanel.NodeStore v1, VisPanel.NodeStore v2) {
        edge.setColor("#f4f4f4");
    }

    protected void updateConnectedEdge(String k1, VisPanel.NodeStore v1, String k2, VisPanel.NodeStore v2) {
        if (v1.k8sObject().getMetadata().getName() == null || v2.k8sObject().getMetadata().getNamespace() == null) return;
        if (v1.k8sObject().getMetadata().getName().equals(v2.k8sObject().getMetadata().getNamespace()))
            panel.processEdge(v1, v2);
    }
}
