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
package de.mhus.kt2l.resources.all;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.ClusterAction;
import de.mhus.kt2l.cluster.ClusterOverviewPanel;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.PanelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClusterCreateAction implements ClusterAction {

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandle(Core core) {
        return true;
    }

    @Override
    public boolean canHandle(Core core, ClusterOverviewPanel.ClusterItem cluster) {
        return true;
    }

    @Override
    public String getTitle() {
        return "Create";
    }

    @Override
    public void execute(Core core, ClusterOverviewPanel.ClusterItem cluster) {
        panelService.addPanel(
                core.getMainTab(),
                cluster.name() + ":"+cluster.config().getDefaultNamespace()+":create",
                cluster.config().getDefaultNamespace(),
                false,
                VaadinIcon.FILE_ADD.create(),
                () ->
                        new ResourceCreatePanel(
                                cluster.config(),
                                core,
                                cluster.config().getDefaultNamespace()
                        )).setHelpContext("create").select();

    }

    @Override
    public Icon getIcon() {
        return VaadinIcon.FILE_ADD.create();
    }

    @Override
    public int getPriority() {
        return 5000;
    }
}
