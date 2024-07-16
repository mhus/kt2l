package de.mhus.kt2l.resources.daemonset;

import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.resources.util.RolloutPanel;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DaemonSetRolloutPanel extends RolloutPanel<V1DaemonSet> {

    public DaemonSetRolloutPanel(Core core, Cluster cluster) {
        super(core, cluster);
    }

    @Override
    protected void updateRunnng(boolean running) {
    }

    @Override
    protected void updateTarget() {
        targetReady = getInt(target.getStatus().getUpdatedNumberScheduled(), -1);
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
