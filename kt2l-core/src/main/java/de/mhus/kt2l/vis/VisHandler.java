package de.mhus.kt2l.vis;

import de.mhus.kt2l.k8s.K8s;
import io.kubernetes.client.common.KubernetesObject;
import org.vaadin.addons.visjs.network.main.Edge;
import org.vaadin.addons.visjs.network.main.Node;

public interface VisHandler {
    void init(VisPanel visPanel);

    void postPrepareNode(Node node);

    void prepareNode(Node node, KubernetesObject res);

    K8s getManagedResourceType();

    void updateEdges(String k1, VisPanel.NodeStore v1);

    void createEdge(Edge edge, VisPanel.NodeStore v1, VisPanel.NodeStore v2);

    void destroy();
}
