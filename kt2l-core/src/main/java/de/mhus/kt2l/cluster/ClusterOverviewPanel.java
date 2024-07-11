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
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.StreamResource;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tree.ITreeNode;
import de.mhus.kt2l.aaa.AaaConfiguration;
import de.mhus.kt2l.aaa.SecurityService;
import de.mhus.kt2l.aaa.SecurityUtils;
import de.mhus.kt2l.config.ViewsConfiguration;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.CoreAction;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.generated.DeployInfo;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.kscript.AttachScope;
import de.mhus.kt2l.system.DevelopmentAction;
import de.mhus.kt2l.ui.UiUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.apache.logging.log4j.util.Strings.isBlank;

@Slf4j
@Configurable
public class ClusterOverviewPanel extends VerticalLayout implements DeskTabListener {

    @Autowired
    private K8sService k8s;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ViewsConfiguration viewsConfiguration;

    @Autowired
    private PanelService panelService;

    private DeskTab tab;

    @Getter
    private Core core;
    private ComboBox<ClusterItem> clusterBox;
    private List<ClusterItem> clusterList;

    private KonamiAction konamiAction = new KonamiAction() {
        public void doAction() {
            UiUtil.showSuccessNotification("You found the Konami Code");
            new DevelopmentAction().execute(panelService, core, false);
        }
    };
    private ITreeNode viewConfig;
    private Div clusterLabel;
    private Cluster defaultCluster;
    private List<ClusterActionRecord> clusterActionList = Collections.synchronizedList(new ArrayList<>());

    public ClusterOverviewPanel(Core core) {
        this.core = core;
    }

    public void createUi() {
        viewConfig = viewsConfiguration.getConfig("clusterOverview");

        add(new Text(" "));

        if (clusterService.isClusterSelectorEnabled()) {
            clusterBox = new ComboBox<>("Select a cluster");
            clusterList = clusterService.getAvailableClusters().stream().map(c -> new ClusterItem(c.getName(), c.getTitle(), c)).toList();
            clusterBox.setItems(clusterList);
            clusterBox.setItemLabelGenerator(ClusterItem::title);
            clusterBox.setWidthFull();
            clusterBox.setId("clusterselect");

            if (viewConfig.getBoolean("colors", true)) {
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
                    updateClusterActions();
                });
            }
            clusterService.defaultClusterName().ifPresent(defaultClusterName -> {
                clusterList.stream().filter(c -> c.name().equals(defaultClusterName)).findFirst().ifPresent(clusterBox::setValue);
            });
            add(clusterBox);
        } else {
            clusterLabel = new Div();
            var clusterName = clusterService.defaultClusterName().get();
            defaultCluster = clusterService.getCluster(clusterName);
            clusterLabel.setText(defaultCluster.getTitle());
            clusterLabel.addClassName("cluster-label");
            add(clusterLabel);
        }
        var clusterActions = clusterService.getClusterActions(core);
        if (!clusterActions.isEmpty()) {
            var clusterMenuBar = new MenuBar();
            add(clusterMenuBar);
            createClusterActions(clusterActions, clusterMenuBar);
        }

        var coreActions = clusterService.getCoreActions(core);
        if (!coreActions.isEmpty()) {
            var coreMenuBar = new MenuBar();
            add(coreMenuBar);
            createCoreActions(coreActions, coreMenuBar);
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

        var supportText = viewsConfiguration.getConfig("clusterOverview").getString("supportText", "KT2L Website");
        var supportLink = viewsConfiguration.getConfig("clusterOverview").getString("supportLink", "https://kt2l.org");
        if (!isBlank(supportText)) {
            var support = new Anchor(supportLink, supportText);
            support.setTarget("_blank");
            add(support);
        }
        var showLicence = viewsConfiguration.getConfig("clusterOverview").getBoolean("showLicence", true);
        if (showLicence) {
            var licence = new Anchor("https://www.gnu.org/licenses/gpl-3.0.html#license-text", "This is open source software and provided \"as is\", without warranties. See  GPLv3 License");
            licence.setTarget("_blank");
            add(licence);
        }

        updateClusterActions();

        setPadding(false);
        setMargin(false);

        setWidthFull();
        if (viewsConfiguration.getConfig("clusterOverview").getBoolean("konami", true))
            konamiAction.attach(version);

    }

    private void updateClusterActions() {
        var cluster = clusterBox.getValue().cluster();
        Thread.startVirtualThread(() -> {
            if (!validateCluster(cluster)) {
                core.ui().access(() -> {
                    clusterActionList.forEach(r -> r.item.setEnabled(false));
                    UiUtil.showErrorNotification("Can't connect to cluster");
                });
                return;
            }
            clusterActionList.forEach(r -> {
                var enabled = r.action.canHandle(core, cluster);
                core.ui().access(() -> r.item.setEnabled(enabled));
            });
        });
    }

    private void createCoreActions(List<CoreAction> coreActions, MenuBar coreMenuBar) {
        coreActions.stream()
                .forEach(action -> {

                    var item = coreMenuBar.addItem(action.getTitle(), click -> {
                        if (getSelectedCluster() != null) {
                            action.execute(core);
                        }
                    });
                    var icon = action.getIcon();
                    if (icon != null)
                        item.addComponentAsFirst(action.getIcon());
                });
    }

    private void createClusterActions(List<ClusterAction> clusterActions, MenuBar clusterMenuBar) {
        clusterActionList.clear();
        clusterActions.stream()
                .forEach(action -> {

                    var item = clusterMenuBar.addItem(action.getTitle(), click -> {
                        if (getSelectedCluster() != null) {
                            if (!validateCluster(getSelectedCluster())) {
                                UiUtil.showErrorNotification("Can't connect to cluster");
                                return;
                            }
                            if (!action.canHandle(core, getSelectedCluster())) {
                                UiUtil.showErrorNotification("Can't handle this cluster");
                                return;
                            }
                            action.execute(core, getSelectedCluster());
                        }
                    });
                    var icon = action.getIcon();
                    if (icon != null)
                        item.addComponentAsFirst(action.getIcon());
                    clusterActionList.add(new ClusterActionRecord(action, item));
                });
    }

    private Cluster getSelectedCluster() {
        return clusterBox != null ? clusterBox.getValue().cluster() : defaultCluster;
    }

    private boolean validateCluster(Cluster cluster) {
        var clusterId = cluster.getName();
        try {
            var coreApi = k8s.getKubeClient(clusterId).getCoreV1Api();
            coreApi.listNamespace(null, null, null, null, null, null, null, null, null, null, null);
            return true;
        } catch (Exception e) {
            LOGGER.warn("Can't connect to cluster: " + clusterId, e);
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

    private record ClusterActionRecord(ClusterAction action, MenuItem item) {
    }
}
