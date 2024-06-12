package de.mhus.kt2l.vis;

import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.rolebinding.RoleBindingWatch;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1RoleBinding;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.vaadin.addons.visjs.network.main.Edge;
import org.vaadin.addons.visjs.network.main.Node;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RoleBindingVis extends AbstractVisHandler {
    @Override
    public K8s[] getConnectedResourceTypes() {
        return new K8s[] {K8s.SERVICE_ACCOUNT};
    }

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return RoleBindingWatch.class;
    }

    @Override
    public void prepareNode(Node node, KubernetesObject res) {
        node.setColor("#ccc");
        node.setMass(1);

    }

    @Override
    public K8s getManagedResourceType() {
        return K8s.ROLE_BINDING;
    }

    @Override
    public void createEdge(Edge edge, VisPanel.NodeStore v1, VisPanel.NodeStore v2) {

    }

    protected void updateConnectedEdge(String k1, VisPanel.NodeStore v1, String k2, VisPanel.NodeStore v2) {
        if (v1.k8sObject().getMetadata().getName() == null || v2.k8sObject().getMetadata().getName() == null) return;
        if (!v1.k8sObject().getMetadata().getNamespace().equals(v2.k8sObject().getMetadata().getNamespace())) return;

        var serviceAccount = v2.k8sObject().getMetadata().getName();
        var roleBinding = (V1RoleBinding)v1.k8sObject();

        for (var subject : roleBinding.getSubjects()) {
            if (subject.getKind().equals("ServiceAccount") && subject.getName().equals(serviceAccount)) {
                panel.processEdge(v1, v2);
                return;
            }
        }
    }
}
