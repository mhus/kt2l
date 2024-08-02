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
package de.mhus.kt2l.resources.deployment;

import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.resources.util.RolloutPanel;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.util.labels.LabelSelector;
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
        ownerLabelSelector = LabelSelector.parse(target.getSpec().getSelector());
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
