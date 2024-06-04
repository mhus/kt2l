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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.StreamResource;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.config.AaaConfiguration;
import de.mhus.kt2l.config.ViewsConfiguration;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.CoreAction;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.core.SecurityUtils;
import de.mhus.kt2l.core.UiUtil;
import de.mhus.kt2l.generated.DeployInfo;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.resources.generic.GenericK8s;
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Configurable
public class ClusterOverviewPanel extends VerticalLayout implements DeskTabListener {

    @Autowired
    private K8sService k8s;

    @Autowired(required = false)
    private ClusterService clusterService;

    @Autowired(required = false)
    private List<ClusterAction> clusterActions;

    @Autowired
    private List<CoreAction> coreActions;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ViewsConfiguration viewsConfiguration;

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
                .filter(cluster -> cluster.cluster().isEnabled())
                .toList();

        clusterBox.setItems(clusterList);
        clusterBox.setItemLabelGenerator(ClusterItem::title);
        clusterBox.setWidthFull();
        clusterBox.setId("clusterselect");

        if (viewsConfiguration.getConfig("clusterOverview").getBoolean("colors", true)) {
            clusterBox.setRenderer(new ComponentRenderer<Component, ClusterItem>(item -> {
                Div div = new Div(item.title());
                if (item.cluster().getColor() != null)
                    div.addClassName("color-" + item.cluster().getColor().name().toLowerCase());
                return div;
            }));
            clusterBox.addValueChangeListener(e -> {
                clusterBox.getClassNames().removeIf(n -> n.startsWith("color-"));
                if (e.getValue() != null && e.getValue().cluster().getColor() != null) {
                    clusterBox.addClassName("color-" + e.getValue().cluster().getColor().name().toLowerCase());
                }
            });
        }
        clusterService.defaultClusterName().ifPresent(defaultClusterName -> {
            clusterList.stream().filter(c -> c.name().equals(defaultClusterName)).findFirst().ifPresent(clusterBox::setValue);
        });
        add(clusterBox);

        if (clusterActions != null) {
            var clusterMenuBar = new MenuBar();
            add(clusterMenuBar);
            final var clusterDefaultRole = securityService.getRolesForResource(AaaConfiguration.SCOPE_DEFAULT, AaaConfiguration.SCOPE_CLUSTER_ACTION);
            clusterActions.stream()
                    .filter(action -> securityService.hasRole(AaaConfiguration.SCOPE_CLUSTER_ACTION, SecurityUtils.getResourceId(action), clusterDefaultRole) && action.canHandle(core))
                    .sorted(Comparator.comparingInt(a -> a.getPriority()))
                    .forEach(action -> {

                        var item = clusterMenuBar.addItem(action.getTitle(), click -> {
                            if (clusterBox.getValue() != null) {
                                if (!validateCluster(clusterBox.getValue())) {
                                    return;
                                }
                                if (!action.canHandle(core, clusterBox.getValue().cluster())) {
                                    UiUtil.showErrorNotification("Can't handle this cluster");
                                    return;
                                }
                                action.execute(core, clusterBox.getValue().cluster());
                            }
                        });
                        var icon = action.getIcon();
                        if (icon != null)
                            item.addComponentAsFirst(action.getIcon());
                    });
        }

        if (coreActions != null) {
            var coreMenuBar = new MenuBar();
            add(coreMenuBar);
            final var coreDefaultRole = securityService.getRolesForResource(AaaConfiguration.SCOPE_DEFAULT, AaaConfiguration.SCOPE_CORE_ACTION);
            coreActions.stream()
                    .filter(action -> securityService.hasRole(AaaConfiguration.SCOPE_CORE_ACTION, SecurityUtils.getResourceId(action), coreDefaultRole) && action.canHandle(core))
                    .sorted(Comparator.comparingInt(a -> a.getPriority()))
                    .forEach(action -> {

                        var item = coreMenuBar.addItem(action.getTitle(), click -> {
                            if (clusterBox.getValue() != null) {
                                if (!validateCluster(clusterBox.getValue())) {
                                    return;
                                }
                                action.execute(core);
                            }
                        });
                        var icon = action.getIcon();
                        if (icon != null)
                            item.addComponentAsFirst(action.getIcon());
                    });
        }

        StreamResource imageResource = new StreamResource("kt2l-logo.svg",
                () -> getClass().getResourceAsStream("/images/kt2l-logo.svg"));

        Image image = new Image(imageResource, "Logo");
        image.setWidthFull();
        image.setMaxWidth("800px");
        add(image);

        Div version = new Div("Version: " + DeployInfo.VERSION + " (" + MString.beforeIndexOrAll(DeployInfo.CREATED, ' ') + ")");
        version.addClassName("version");
        add(version);
        setPadding(false);
        setMargin(false);

        setWidthFull();
    }

    private boolean validateCluster(ClusterItem cluster) {
        var clusterId = cluster.cluster().getName();
        try {
            var coreApi = k8s.getKubeClient(clusterId).getCoreV1Api();
            coreApi.listNamespace(null, null, null, null, null, null, null, null, null, null, null);
            return true;
        } catch (Exception e) {
            LOGGER.error("Can't connect to cluster: " + clusterId, e);
            UiUtil.showErrorNotification("Can't connect to cluster: " + clusterId, e);
        }
        return false;
    }

    @Override
    public void tabInit(DeskTab deskTab) {
        LOGGER.debug("Main Init");
        this.tab = deskTab;
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

    public record ClusterItem(String name, String title, Cluster cluster) {
    }

}
