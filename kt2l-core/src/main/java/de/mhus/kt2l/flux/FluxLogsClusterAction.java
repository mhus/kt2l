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
package de.mhus.kt2l.flux;

import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.server.StreamResource;
import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterAction;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.helm.HelmClusterAction;
import de.mhus.kt2l.resources.pod.PodLogsPanel;
import de.mhus.kt2l.ui.UiUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
@WithRole(UsersConfiguration.ROLE.READ)
public class FluxLogsClusterAction implements ClusterAction {

    private static final String SELECTOR = "app.kubernetes.io/part-of=flux";
    @Autowired
    private PanelService panelService;

    private static final String FLUX_NAMESPACE = "flux-system";

    @Override
    public boolean canHandle(Core core) {
        return true;
    }

    @Override
    public boolean canHandle(Core core, Cluster cluster) {
        try {
            var ns = cluster.getApiProvider().getCoreV1Api().readNamespace(FLUX_NAMESPACE, null);
            return ns != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getTitle() {
        return "Flux Logs";
    }

    @Override
    public void execute(Core core, Cluster cluster) {
        try {
            var list = cluster.getApiProvider().getCoreV1Api().listNamespacedPod(FLUX_NAMESPACE, null, null, null, null, null, null, null, null, null, null, null);
            var selected = list.getItems().stream().filter(p -> p.getMetadata().getName().contains("-controller-")).collect(Collectors.toSet());
            var tab = panelService.addPodLogsPanel(core.getHomeTab(), core, cluster, selected)
                    .setTabTitle("Flux Logs")
                    .setWindowTitle("Flux Logs")
                    .select();
            tab.setColor(cluster.getColor());
            ((PodLogsPanel) tab.getPanel()).setShowSource(false);
            ((PodLogsPanel) tab.getPanel()).setJsonFields("ts,level,controller,msg");
        } catch (Exception e) {
            LOGGER.error("Can't get Flux logs", e);
            UiUtil.showErrorNotification("Can't get Flux logs", e);
        }
    }

    @Override
    public AbstractIcon getIcon() {
        var icon = createIcon();
        icon.addClassName("icon-for-button-with-text");
        return icon;
    }

    @Override
    public int getPriority() {
        return 3406;
    }

    public static SvgIcon createIcon() {
        StreamResource iconResource = new StreamResource("flux-logo.svg",
                () -> HelmClusterAction.class.getResourceAsStream("/images/flux-logo.svg"));
        SvgIcon icon = new SvgIcon(iconResource);
        return icon;
    }

}
