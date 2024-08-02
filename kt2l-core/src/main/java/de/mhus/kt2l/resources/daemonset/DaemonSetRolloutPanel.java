/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l.resources.daemonset;

import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.resources.util.RolloutPanel;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.util.labels.LabelSelector;
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
        return DaemonSetWatch.class;
    }
}
