package de.mhus.kt2l.vis;

import de.mhus.commons.lang.IRegistration;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.addons.visjs.network.main.Node;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractVisHandler implements VisHandler {

    protected VisPanel panel;
    private HandlerK8s k8sHandler;
    private IRegistration eventRegistration;

    public void init(VisPanel visPanel) {
        this.panel = visPanel;
        k8sHandler = visPanel.getK8sHandler(getManagedResourceType());

        if (getManagedWatchClass() != null)
            eventRegistration = panel.getCore().backgroundJobInstance(panel.getCluster(), getManagedWatchClass()).getEventHandler().registerWeak(this::changeEvent);

        try {
            var allResList = k8sHandler.createResourceListWithoutNamespace(visPanel.getCluster().getApiProvider());
            allResList.getItems().forEach(res -> {
                panel.processNode(this, res);
            });
        } catch (Exception e) {
            LOGGER.warn("Error", e);
        }
    }

    public void destroy() {
        if (eventRegistration != null)
            eventRegistration.unregister();
    }

    private void changeEvent(Watch.Response<KubernetesObject> event) {
        if (K8sUtil.WATCH_EVENT_DELETED.equals(event.type))
            panel.deleteNode(this, event.object);
        else
            panel.processNode(this, event.object);
    }

    public void updateEdges(String k1, VisPanel.NodeStore v1) {
        var connectedKinds = Arrays.stream(getConnectedResourceTypes()).map(t -> t.kind() ).collect(Collectors.toSet());
        panel.getNodes().forEach((k2, v2) -> {
            var kind = VisPanel.getKindOfNodId(k2);
            if (kind == null) return;
            if (connectedKinds.contains(kind)) {
                updateConnectedEdge(k1, v1, k2, v2);
            }
        });
    }

    protected void updateConnectedEdge(String k1, VisPanel.NodeStore v1, String k2, VisPanel.NodeStore v2) {
        var uid = v1.k8sObject().getMetadata().getUid();
        var ownerReferences = v2.k8sObject().getMetadata().getOwnerReferences();
        if (ownerReferences != null) {
            ownerReferences.forEach(ref -> {
                if (ref.getUid().equals(uid)) {
                    panel.processEdge(v1, v2);
                }
            });
        }
    }

    public abstract K8s[] getConnectedResourceTypes();

    protected abstract Class<? extends ClusterBackgroundJob> getManagedWatchClass();

    public void postPrepareNode(Node node) {
    }
}
