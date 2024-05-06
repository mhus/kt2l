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

package de.mhus.kt2l.cluster;

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.config.AaaConfiguration;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.core.SecurityUtils;
import de.mhus.kt2l.core.UiUtil;
import de.mhus.kt2l.generated.DeployInfo;
import de.mhus.kt2l.k8s.K8sService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.List;

@Slf4j
public class ClusterOverviewPanel extends VerticalLayout implements DeskTabListener {

    @Autowired
    private K8sService k8s;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private List<ClusterAction> clusterActions;

    @Autowired
    private SecurityService securityService;

    private DeskTab tab;

    @Getter
    private Core core;
    private ComboBox<ClusterItem> clusterBox;
    private List<ClusterItem> clusterList;

    public ClusterOverviewPanel(Core core) {
        this.core = core;
    }

    public void createUi() {

        add(new Text(" "));
        clusterBox = new ComboBox<>("Select a cluster");
        clusterList = k8s.getAvailableContexts().stream()
                .map(name -> {
                    final var cluster = clusterService.getCluster(name);
                    return new ClusterItem(name, cluster.getTitle(), cluster);
                })
                .filter(cluster -> cluster.config().isEnabled())
                .toList();

        clusterBox.setItems(clusterList);
        clusterBox.setItemLabelGenerator(ClusterItem::title);
        clusterBox.setWidthFull();
        clusterService.defaultClusterName().ifPresent(defaultClusterName -> {
            clusterList.stream().filter(c -> c.name().equals(defaultClusterName)).findFirst().ifPresent(clusterBox::setValue);
        });
        add(clusterBox);

        var menuBar = new MenuBar();
        add(menuBar);

        final var defaultRole = securityService.getRolesForResource(AaaConfiguration.SCOPE_DEFAULT, AaaConfiguration.SCOPE_CLUSTER_ACTION);
        clusterActions.stream()
                .filter(action -> securityService.hasRole(AaaConfiguration.SCOPE_CLUSTER_ACTION, SecurityUtils.getResourceId(action), defaultRole ))
                .sorted(Comparator.comparingInt(a -> a.getPriority()))
                .forEach(action -> {

            var item = menuBar.addItem(action.getTitle(), click -> {
                if (clusterBox.getValue() != null) {
                    if (!validateCluster(clusterBox.getValue())) {
                        return;
                    }
                    action.execute(core, clusterBox.getValue());
                }
            });
            var icon = action.getIcon();
            if (icon != null)
                item.addComponentAsFirst(action.getIcon());
        });

        StreamResource imageResource = new StreamResource("kt2l-logo.svg",
                () -> getClass().getResourceAsStream("/images/kt2l-logo.svg"));

        Image image = new Image(imageResource, "Logo");
        image.setWidthFull();
        image.setMaxWidth("800px");
        add(image);

        Div version = new Div("Version: " + DeployInfo.VERSION + " (" + MString.beforeIndexOrAll(DeployInfo.CREATED, ' ') + ")");
        version.addClassName("version");
        add(version);

        setWidthFull();
    }

    private boolean validateCluster(ClusterItem cluster) {
        var clusterId = cluster.config().getName();
        try {
            var coreApi = k8s.getKubeClient(clusterId).getCoreV1Api();
            coreApi.listNamespace().execute();
            return true;
        } catch (Exception e) {
            LOGGER.error("Can't connect to cluster: " + clusterId, e);
            UiUtil.showErrorNotification("Can't connect to cluster: " + clusterId, e);
        }
        return false;
    }

    @Override
    public void tabInit(DeskTab xTab) {
        LOGGER.debug("Main Init");
        this.tab = xTab;
        createUi();
    }

    @Override
    public void tabSelected() {
        LOGGER.debug("Main Selected");
        if (clusterBox != null)
            clusterBox.focus();
    }

    @Override
    public void tabUnselected() {
        LOGGER.debug("Main DeSelected");
    }

    @Override
    public void tabDestroyed() {
        LOGGER.debug("Main Destroyed");
    }

    @Override
    public void tabRefresh(long counter) {
        LOGGER.trace("Main Refreshed");
    }

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }

    public record ClusterItem(String name, String title, Cluster config) {
    }

}
