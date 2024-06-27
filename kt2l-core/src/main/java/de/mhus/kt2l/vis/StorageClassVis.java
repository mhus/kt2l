package de.mhus.kt2l.vis;

import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.persistentvolume.PersistentVolumeWatch;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.vaadin.addons.visjs.network.main.Edge;
import org.vaadin.addons.visjs.network.main.Node;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class StorageClassVis extends  AbstractVisHandler {
    @Override
    public V1APIResource[] getConnectedTypes() {
        return new V1APIResource[] {K8s.PERSISTENT_VOLUME_CLAIM, K8s.PERSISTENT_VOLUME};
    }

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return PersistentVolumeWatch.class;
    }

    @Override
    public void prepareNode(Node node, KubernetesObject res) {
        node.setColor("#eeaafe");
        node.setMass(2);
    }

    @Override
    public V1APIResource getType() {
        return K8s.STORAGE_CLASS;
    }

    @Override
    public void createEdge(Edge edge, VisPanel.NodeStore v1, VisPanel.NodeStore v2) {
    }

    protected void updateConnectedEdge(String k1, VisPanel.NodeStore v1, String k2, VisPanel.NodeStore v2) {
        if (v1.k8sObject().getMetadata().getName() == null)
            return;

        var resName = v1.k8sObject().getMetadata().getName();
        if (v2.k8sObject() instanceof V1PersistentVolumeClaim claim && claim.getSpec().getStorageClassName() != null) {
            if (claim.getSpec().getStorageClassName().equals(resName)) {
                panel.processEdge(v1, v2);
            }
        }

        if (v2.k8sObject() instanceof V1PersistentVolume volume && volume.getSpec().getStorageClassName() != null) {
            if (volume.getSpec().getStorageClassName().equals(resName)) {
                panel.processEdge(v1, v2);
            }
        }

    }
}
