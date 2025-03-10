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
package de.mhus.kt2l.helm;

import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.server.StreamResource;
import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterAction;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.k8s.K8sService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@WithRole(UsersConfiguration.ROLE.READ)
public class HelmClusterAction implements ClusterAction {

    @Autowired
    private K8sService k8sService;

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandle(Core core) {
        return true;
    }

    @Override
    public boolean canHandle(Core core, Cluster cluster) {
        try {
            var fieldSelector = "type=helm.sh/release.v1";
            var list = cluster.getApiProvider().getCoreV1Api().listSecretForAllNamespaces().fieldSelector(fieldSelector).execute();
            return list.getItems().size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getTitle() {
        return "Helm";
    }

    @Override
    public void execute(Core core, Cluster cluster) {
        panelService.showHelmInstalledChartsPanel(core, cluster).select();
    }

    @Override
    public AbstractIcon getIcon() {
        var icon = createIcon();
        icon.addClassName("icon-for-button-with-text");
        return icon;
    }

    @Override
    public int getPriority() {
        return 3405;
    }


    public static SvgIcon createIcon() {
        StreamResource iconResource = new StreamResource("helm-logo.svg",
                () -> HelmClusterAction.class.getResourceAsStream("/images/helm-logo.svg"));
        SvgIcon icon = new SvgIcon(iconResource);
        return icon;
    }
}
