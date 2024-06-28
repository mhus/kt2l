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
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.config.ViewsConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@WithRole(UsersConfiguration.ROLE.ADMIN)
public class LocalBashCoreAction implements CoreAction {

    @Autowired
    private PanelService panelService;

    @Autowired
    private ViewsConfiguration viewsConfiguration;

    @Override
    public boolean canHandle(Core core) {
        var bash = viewsConfiguration.getConfig("localBash").getString("path", "/bin/bash");
        return new File(bash).canExecute();
    }

    @Override
    public String getTitle() {
        return "Local Bash";
    }

    @Override
    public void execute(Core core) {
        panelService.addLocalBashPanel(core).select();
    }

    @Override
    public Icon getIcon() {
        return VaadinIcon.MODAL.create();
    }

    @Override
    public int getPriority() {
        return 2050;
    }
}
