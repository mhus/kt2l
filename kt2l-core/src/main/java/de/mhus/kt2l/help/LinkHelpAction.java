/**
 * This file is part of kt2l-core.
 *
 * kt2l-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * kt2l-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with kt2l-core.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l.help;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.commons.tools.MSystem;
import de.mhus.kt2l.core.MainView;
import org.springframework.stereotype.Component;

@Component
public class LinkHelpAction implements HelpAction {
    @Override
    public boolean canHandle(HelpConfiguration.HelpLink link) {
        return "link".equals(link.getAction());
    }

    @Override
    public void execute(MainView mainView, HelpConfiguration.HelpLink link) {
        link.getNode().getString("href").ifPresent(href ->
                MSystem.openBrowserUrl(href)
        );
    }

    @Override
    public Icon getIcon(HelpConfiguration.HelpLink link) {
        return VaadinIcon.EXTERNAL_LINK.create();
    }
}
