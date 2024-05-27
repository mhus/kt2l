package de.mhus.kt2l.vis;

import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.pod.PodWatch;
import io.kubernetes.client.common.KubernetesObject;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.vaadin.addons.visjs.network.main.Edge;
import org.vaadin.addons.visjs.network.main.Node;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PodVis extends AbstractVisHandler {

    @Override
    public K8s[] getConnectedResourceTypes() {
        return new K8s[0];
    }

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return PodWatch.class;
    }

    @Override
    public void prepareNode(Node node, KubernetesObject res) {
        node.setColor("#ffffde");
        node.setMass(4);
    }

    @Override
    public K8s getManagedResourceType() {
        return K8s.POD;
    }

    @Override
    public void createEdge(Edge edge, VisPanel.NodeStore v1, VisPanel.NodeStore v2) {

    }
}
