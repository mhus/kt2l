package de.mhus.kt2l.vis;

import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.persistentvolumeclaim.PersistentVolumeClaimWatch;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.vaadin.addons.visjs.network.main.Edge;
import org.vaadin.addons.visjs.network.main.Node;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PersistentVolumeClaimVis extends  AbstractVisHandler {
    @Override
    public V1APIResource[] getConnectedTypes() {
        return new V1APIResource[] {K8s.POD};
    }

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return PersistentVolumeClaimWatch.class;
    }

    @Override
    public void prepareNode(Node node, KubernetesObject res) {
        node.setColor("#eefffe");
        node.setMass(2);
    }

    @Override
    public V1APIResource getType() {
        return K8s.PERSISTENT_VOLUME_CLAIM;
    }

    @Override
    public void createEdge(Edge edge, VisPanel.NodeStore v1, VisPanel.NodeStore v2) {
    }

    protected void updateConnectedEdge(String k1, VisPanel.NodeStore v1, String k2, VisPanel.NodeStore v2) {
        if (v1.k8sObject().getMetadata().getName() == null || !(v2.k8sObject() instanceof V1Pod))
            return;
        if (!v1.k8sObject().getMetadata().getNamespace().equals(v2.k8sObject().getMetadata().getNamespace()))
            return;

        var pod = (V1Pod) v2.k8sObject();
        if (pod.getSpec().getVolumes() == null) return;
        var resName = v1.k8sObject().getMetadata().getName();

        pod.getSpec().getVolumes().forEach(volume -> {
            if (volume.getPersistentVolumeClaim() != null) {
                if (volume.getPersistentVolumeClaim().getClaimName().equals(resName)) {
                    panel.processEdge(v1, v2);
                }
            }
        });

    }
}
