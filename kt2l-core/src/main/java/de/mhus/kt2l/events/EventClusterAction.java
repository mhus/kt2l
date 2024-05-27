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
package de.mhus.kt2l.events;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.ClusterAction;
import de.mhus.kt2l.cluster.ClusterOverviewPanel;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.PanelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventClusterAction implements ClusterAction {

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
        return "Events";
    }

    @Override
    public void execute(Core core, ClusterOverviewPanel.ClusterItem cluster) {
        var name = cluster.name();
        panelService.addPanel(
                core, cluster.cluster(),
                name + ":events",
                cluster.title(),
                false,
                VaadinIcon.CALENDAR_CLOCK.create(),
                () -> new EventPanel(core, cluster.cluster())
        ).setHelpContext("events").setWindowTitle(cluster.title() + " Events").select();

    }

    @Override
    public Icon getIcon() {
        return VaadinIcon.CALENDAR_CLOCK.create();
    }

    @Override
    public int getPriority() {
        return 10000;
    }
}
