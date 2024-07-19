package de.mhus.kt2l.resources.statefulset;

import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.resources.util.RolloutPanel;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.util.labels.LabelSelector;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StatefulSetRolloutPanel extends RolloutPanel<V1StatefulSet> {

    public StatefulSetRolloutPanel(Core core, Cluster cluster) {
        super(core, cluster);
    }

    @Override
    protected void updateRunnng(boolean running) {
    }

    @Override
    protected void updateTarget() {
        targetReady = getInt(target.getStatus().getUpdatedReplicas(), -1);
        targetDesired = getInt(target.getSpec().getReplicas(), -1);
        targetUnavailable = -1;
        ownerLabelSelector = LabelSelector.parse(target.getSpec().getSelector());
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
