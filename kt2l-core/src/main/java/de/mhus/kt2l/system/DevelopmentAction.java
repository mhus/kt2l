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
package de.mhus.kt2l.system;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.commons.tools.MSystem;
import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.CoreAction;
import de.mhus.kt2l.core.PanelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
@WithRole(UsersConfiguration.ROLE.ADMIN)
public class DevelopmentAction implements CoreAction {

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandle(Core core) {
        return MSystem.isVmDebug();
    }

    @Override
    public String getTitle() {
        return "Development";
    }

    @Override
    public void execute(Core core) {
        execute(panelService, core, true);
    }

    public void execute(PanelService panelService, Core core, boolean evilMode) {
        panelService.addPanel(core, null, "development", "Development", true, VaadinIcon.HAMMER.create(), () ->
                new DevelopmentPanel(evilMode)
        ).setReproducable(true).select();
    }

    @Override
    public Icon getIcon() {
        return VaadinIcon.HAMMER.create();
    }

    @Override
    public int getPriority() {
        return 10000;
    }
}
