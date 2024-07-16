package de.mhus.kt2l.resources.daemonset;

import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.resources.deployment.DeploymentK8s;
import de.mhus.kt2l.resources.deployment.DeploymentWatch;
import de.mhus.kt2l.resources.util.RolloutPanel;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Deployment;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DaemonSetRolloutPanel extends RolloutPanel<V1DaemonSet> {

    @Setter
    private DeploymentK8s handler;

    public DaemonSetRolloutPanel(Core core, Cluster cluster) {
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
        targetUpdated = getInt(target.getStatus().getUpdatedNumberScheduled(), -1);
        targetDesired = getInt(target.getStatus().getDesiredNumberScheduled(), -1);
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
        return DaemonSetWatch.class;
    }
}
