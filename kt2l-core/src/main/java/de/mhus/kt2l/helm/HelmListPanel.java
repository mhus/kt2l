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

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.k8s.K8sService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

public class HelmListPanel extends VerticalLayout implements DeskTabListener {

    @Autowired
    private K8sService k8sService;

    private final Core core;
    private final Cluster cluster;
    private Grid<HelmResource> grid;

    public HelmListPanel(Core core, Cluster cluster) {
        this.core = core;
        this.cluster = cluster;
    }

    @Override
    public void tabInit(DeskTab deskTab) {

        var menuBar = new MenuBar();
        menuBar.addItem("Refresh", e -> refresh());
        add(menuBar);

        grid = new Grid<HelmResource>();
        grid.addColumn(HelmResource::getName).setHeader("Name");
        grid.setSizeFull();
        add(grid);

        refresh();
        setSizeFull();
    }

    private void refresh() {

    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabUnselected() {

    }

    @Override
    public void tabDestroyed() {
        
    }

    @Override
    public void tabRefresh(long counter) {

    }

    @Getter
    private class HelmResource {

        private String name;

        public HelmResource(String name) {
            this.name = name;
        }

    }

}
