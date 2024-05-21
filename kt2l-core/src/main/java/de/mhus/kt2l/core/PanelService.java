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

package de.mhus.kt2l.core;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterOverviewPanel;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.common.ResourceYamlEditorPanel;
import de.mhus.kt2l.resources.ResourcesGridPanel;
import io.kubernetes.client.common.KubernetesObject;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class PanelService {

    public DeskTab addPanel(
            DeskTab parentTab,
            String id, String title, boolean unique, Icon icon, Supplier<com.vaadin.flow.component.Component> panelCreator) {
        return parentTab.getViewer().addTab(
                id,
                title,
                true,
                unique,
                icon,
                panelCreator)
        .setColor(parentTab.getColor()).setParentTab(parentTab);
    }

    public DeskTab addPanel(
            Core core, ClusterOverviewPanel.ClusterItem cluster,
            String id, String title, boolean unique, Icon icon, Supplier<com.vaadin.flow.component.Component> panelCreator) {
        return core.getTabBar().addTab(
                        id,
                        title,
                        true,
                        unique,
                        icon,
                        panelCreator)
                .setColor(cluster == null ? UiUtil.COLOR.NONE : cluster.config().getColor());
    }

    public DeskTab addDetailsPanel(DeskTab parentTab, Cluster cluster, K8s resourceType, KubernetesObject resource) {
        return parentTab.getViewer().addTab(
                cluster.getName() + ":" + resourceType + ":" + resource.getMetadata().getName() + ":details",
                resource.getMetadata().getName(),
                true,
                true,
                VaadinIcon.FILE_TEXT_O.create(),
                () ->
                        new ResourceYamlEditorPanel(
                                cluster,
                                parentTab.getViewer().getCore(),
                                resourceType,
                                resource
                        )).setColor(parentTab.getColor()).setParentTab(parentTab).setHelpContext("details");

    }

    public DeskTab addResourcesGrid(Core core, ClusterOverviewPanel.ClusterItem cluster) {
        return core.getTabBar().addTab(
                        "test/" + cluster.name(),
                        cluster.title(),
                        true,
                        false,
                        VaadinIcon.OPEN_BOOK.create(),
                        () -> new ResourcesGridPanel(cluster.name(), core))
                .setColor(cluster.config().getColor()).setHelpContext("resources");

    }
}
