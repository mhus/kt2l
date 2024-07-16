package de.mhus.kt2l.resources.statefulset;

import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.resources.deployment.DeploymentK8s;
import de.mhus.kt2l.resources.deployment.DeploymentWatch;
import de.mhus.kt2l.resources.util.RolloutPanel;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StatefulSetRolloutPanel extends RolloutPanel<V1StatefulSet> {

    @Setter
    private DeploymentK8s handler;

    public StatefulSetRolloutPanel(Core core, Cluster cluster) {
        super(core, cluster);
    }

    @Override
    protected void updateRunnng(boolean running) {
        try {
            handler.patch(cluster.getApiProvider(), target, "[{\"op\":\"replace\",\"path\":\"/spec/paused\",\"value\":" + (!running ? "true" : "false") + "}]");
            targetStarted = running;
        } catch (ApiException e) {
            UiUtil.showErrorNotification("Can't update deployment", e);
        }
    }

    @Override
    protected void updateTarget() {
        targetUpdated = getInt(target.getStatus().getUpdatedReplicas(), -1);
        targetDesired = getInt(target.getSpec().getReplicas(), -1);
        targetUnavailable = -1;
        ownerNamespace = target.getMetadata().getNamespace();
        targetCanPause = false;
    }

    private boolean getBoolean(Boolean value, boolean def) {
        return value == null ? def : value;
    }

    private int getInt(Integer value, int def) {
        return value == null ? def : value;
    }

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return StatefulSetWatch.class;
    }
}
