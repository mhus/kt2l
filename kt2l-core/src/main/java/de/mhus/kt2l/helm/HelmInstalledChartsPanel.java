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

import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.commons.lang.IRegistration;
import de.mhus.commons.tools.MCast;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.secret.SecretWatch;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.util.Watch;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Slf4j
public class HelmInstalledChartsPanel extends VerticalLayout implements DeskTabListener {

    @Autowired
    private K8sService k8sService;

    private final Core core;
    private final Cluster cluster;
    private Grid<HelmResource> grid;
    private IRegistration registration;
    private Set<HelmResource> installedCharts = new HashSet<>();
    private Set<HelmResource> filteredCharts = new HashSet<>();
    private MenuItem allItemsMenu;

    public HelmInstalledChartsPanel(Core core, Cluster cluster) {
        this.core = core;
        this.cluster = cluster;
    }

    @Override
    public void tabInit(DeskTab deskTab) {

        var menuBar = new MenuBar();
//        menuBar.addItem("Refresh", e -> refresh());
        var viewItem = menuBar.addItem("View");
        var viewSub = viewItem.getSubMenu();
        allItemsMenu = viewSub.addItem("All Versions", event -> {
            filterCharts();
            grid.setItems(filteredCharts);
            grid.getDataProvider().refreshAll();
        });
        allItemsMenu.setCheckable(true);
        allItemsMenu.setChecked(false);
        add(menuBar);

        grid = new Grid<HelmResource>();
        grid.addColumn(HelmResource::getNamespace).setHeader("Namespace");
        grid.addColumn(HelmResource::getName).setHeader("Name");
        grid.addColumn(HelmResource::getVersion).setHeader("Version");
        grid.addColumn(HelmResource::getCreated).setHeader("Created");
        grid.setSizeFull();
        add(grid);

        refreshGrid();
        setSizeFull();

        registration = core.backgroundJobInstance(cluster, SecretWatch.class).getEventHandler().registerWeak(this::updateEvent);

    }

    private void updateEvent(Watch.Response<V1Secret> event) {
        if (event.object == null || !event.object.getType().equals("helm.sh/release.v1"))
            return;

        if (event.type.equals(K8sUtil.WATCH_EVENT_DELETED)) {
            installedCharts.remove(new HelmResource(event.object));
        } else {
            var res = new HelmResource(event.object);
            if (installedCharts.contains(res))
                return;
            installedCharts.add(res);
        }
        filterCharts();
        core.ui().access(() -> {
            grid.setItems(filteredCharts);
            grid.getDataProvider().refreshAll();
        });
    }

    private void refreshGrid() {
        try {
            var fieldSelector = "type=helm.sh/release.v1";
            var list = cluster.getApiProvider().getCoreV1Api().listSecretForAllNamespaces(null, null, fieldSelector, null, null, null, null, null, null, null, null);
            installedCharts.clear();
            for (var item : list.getItems()) {
                installedCharts.add(new HelmResource(item));
            }
            filterCharts();
            grid.setItems(filteredCharts);
            grid.getDataProvider().refreshAll();
        } catch (Exception e) {
            LOGGER.error("Can't fetch helm secrets", e);
        }
    }

    private synchronized  void filterCharts() {
        var filtered = new HashSet<HelmResource>();
        for (var chart : installedCharts) {
            if (allItemsMenu.isChecked()) {
                filtered.add(chart);
            } else {
                var maybeExists = filtered.stream().filter(c -> c.getNamespace().equals(chart.getNamespace()) && c.getName().equals(chart.getName())).findFirst();
                if (maybeExists.isEmpty() || chart.getVersion() > maybeExists.get().getVersion()) {
                    if (maybeExists.isPresent())
                        filtered.remove(maybeExists.get());
                    filtered.add(chart);
                }
            }
        }
        filteredCharts = filtered;
    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabUnselected() {

    }

    @Override
    public void tabDestroyed() {
        registration.unregister();
    }

    @Override
    public void tabRefresh(long counter) {

    }

    @Getter
    private class HelmResource {

        private final V1Secret resource;
        private final int version;
        private final String namespace;
        private final OffsetDateTime created;
        private final String name;

        public HelmResource(V1Secret resource) {
            this.resource = resource;
            var name = resource.getMetadata().getName();
            namespace = resource.getMetadata().getNamespace();
            created = resource.getMetadata().getCreationTimestamp();
            if (name.startsWith("sh.helm.release.v1."))
                name = name.substring("sh.helm.release.v1.".length());
            int pos = name.lastIndexOf('.');
            if (pos >= 0) {
                version = MCast.toint(name.substring(pos + 2), 0);
                name = name.substring(0, pos);
            } else {
                version = 0;
            }
            this.name = name;
        }

        public boolean equals(Object obj) {
            if (obj instanceof HelmResource other) {
                return other.resource.getMetadata().getName().equals(resource.getMetadata().getName())
                        && other.resource.getMetadata().getNamespace().equals(resource.getMetadata().getNamespace());
            }
            if (obj instanceof V1Secret other) {
                return other.getMetadata().getName().equals(resource.getMetadata().getName())
                        && other.getMetadata().getNamespace().equals(resource.getMetadata().getNamespace());
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(resource.getMetadata().getName().hashCode(), resource.getMetadata().getNamespace().hashCode());
        }


    }

}
