package de.mhus.kt2l.resources.replicaset;

import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.resources.util.RolloutPanel;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReplicaSetRolloutPanel extends RolloutPanel<V1ReplicaSet> {

    public ReplicaSetRolloutPanel(Core core, Cluster cluster) {
        super(core, cluster);
    }

    @Override
    protected void updateRunnng(boolean running) {
    }

    @Override
    protected void updateTarget() {
        targetReady = getInt(target.getStatus().getReadyReplicas(), -1);
        targetDesired = getInt(target.getSpec().getReplicas(), -1);
        targetUnavailable = -1;
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
        return ReplicaSetWatch.class;
    }
}
