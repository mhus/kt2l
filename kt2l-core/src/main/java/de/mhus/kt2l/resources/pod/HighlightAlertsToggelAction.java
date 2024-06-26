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
package de.mhus.kt2l.resources.pod;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.resources.pod.score.PodScorerConfiguration;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@WithRole(UsersConfiguration.ROLE.READ)
public class HighlightAlertsToggelAction implements ResourceAction {

    @Autowired
    private PodScorerConfiguration podScorerConfiguration;

    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return K8s.POD.equals(type) && podScorerConfiguration.isEnabled();
    }

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {
        return canHandleType(cluster, type);
    }

    @Override
    public void execute(ExecutionContext context) {
        if (context.getGrid() instanceof PodGrid podGrid) {
            podGrid.setHighlightAlerts(!podGrid.isHighlightAlerts());
            podGrid.doRefreshGrid();
            UiUtil.showSuccessNotification("Highlighting alerts " + (podGrid.isHighlightAlerts() ? "enabled" : "disabled"));
        }
    }

    @Override
    public String getTitle() {
        return "Highlight Alerts;icon=" + VaadinIcon.ALARM;
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.VIEW_PATH;
    }

    @Override
    public int getMenuOrder() {
        return 50000;
    }

    @Override
    public String getShortcutKey() {
        return "CTRL+A";
    }

    @Override
    public String getDescription() {
        return "Toggle Highlight Alerts";
    }
}
