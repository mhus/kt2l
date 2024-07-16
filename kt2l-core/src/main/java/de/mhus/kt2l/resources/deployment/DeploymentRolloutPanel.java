package de.mhus.kt2l.resources.deployment;

import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.resources.util.RolloutPanel;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Deployment;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeploymentRolloutPanel extends RolloutPanel<V1Deployment> {

    @Setter
    private DeploymentK8s handler;

    public DeploymentRolloutPanel(Core core, Cluster cluster) {
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
        targetReady = getInt(target.getStatus().getUpdatedReplicas(), -1);
        targetDesired = getInt(target.getSpec().getReplicas(), -1);
        targetUnavailable = getInt(target.getStatus().getUnavailableReplicas(), 0);
        ownerKind = "Deployment";
        ownerId = target.getMetadata().getUid();
        ownerNamespace = target.getMetadata().getNamespace();
        targetCanPause = true;
        targetStarted = !getBoolean(target.getSpec().getPaused(), false );
    }

    private boolean getBoolean(Boolean value, boolean def) {
        return value == null ? def : value;
    }

    private int getInt(Integer value, int def) {
        return value == null ? def : value;
    }

    @Override
    protected Class<? extends ClusterBackgroundJob> getManagedWatchClass() {
        return DeploymentWatch.class;
    }
}
