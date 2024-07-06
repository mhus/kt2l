/*
 * kt2l-desktop - kt2l desktop app
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
package de.mhus.kt2l;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.CoreAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@WithRole(UsersConfiguration.ROLE.READ)
public class NewWindowAction implements CoreAction  {

    @Autowired
    private BrowserBean browserBean;

    @Override
    public boolean canHandle(Core core) {
        return true;
    }

    @Override
    public String getTitle() {
        return "New Window";
    }

    @Override
    public void execute(Core core) {
        browserBean.openNewWindow();
    }

    @Override
    public Icon getIcon() {
        return VaadinIcon.PLUS.create();
    }

    @Override
    public int getPriority() {
        return 5000;
    }
}
