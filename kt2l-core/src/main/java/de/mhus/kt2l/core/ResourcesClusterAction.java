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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ResourcesClusterAction implements ClusterAction {

    @Autowired
    private PanelService panelService;

    @Override
    public String getTitle() {
        return "Resources";
    }

    @Override
    public void execute(Core core, ClusterOverviewPanel.Cluster cluster) {
        panelService.addResourcesGrid(core, cluster).select();
    }

    @Override
    public Icon getIcon() {
        return VaadinIcon.OPEN_BOOK.create();
    }

    @Override
    public int getPriority() {
        return 1000;
    }
}
