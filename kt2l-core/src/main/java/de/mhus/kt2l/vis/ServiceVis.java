package de.mhus.kt2l.vis;

import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.service.ServiceWatch;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Service;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.vaadin.addons.visjs.network.main.Edge;
import org.vaadin.addons.visjs.network.main.Node;
import org.vaadin.addons.visjs.network.options.edges.ArrowHead;
import org.vaadin.addons.visjs.network.options.edges.Arrows;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ServiceVis extends AbstractVisHandler {
    @Override
    public K8s[] getConnectedResourceTypes() {
        return new K8s[] {K8s.POD};
    }

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return ServiceWatch.class;
    }

    @Override
    public void prepareNode(Node node, KubernetesObject res) {
        node.setColor("#deffde");
        node.setMass(2);
    }

    @Override
    public K8s getManagedResourceType() {
        return K8s.SERVICE;
    }

    @Override
    public void createEdge(Edge edge, VisPanel.NodeStore v1, VisPanel.NodeStore v2) {
        edge.setArrows(new Arrows(new ArrowHead()));
    }

    protected void updateConnectedEdge(String k1, VisPanel.NodeStore v1, String k2, VisPanel.NodeStore v2) {
        if (((V1Service)v1.k8sObject()).getSpec().getSelector() == null || v2.k8sObject().getMetadata().getLabels() == null) return;
        if (K8sUtil.matchLabels(((V1Service)v1.k8sObject()).getSpec().getSelector(), v2.k8sObject().getMetadata().getLabels()))
            panel.processEdge(v1, v2);
    }

}
