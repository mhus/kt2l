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
package de.mhus.kt2l.help;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.core.Core;
import org.springframework.stereotype.Component;

@Component
public class YamlSnippetsHelperAction implements HelpAction {

    @Override
    public boolean canHandle(HelpConfiguration.HelpLink link) {
        return "yaml-snippets".equals(link.getAction());
    }

    @Override
    public void execute(Core core, HelpConfiguration.HelpLink link) {
        YamlSnippetsHelpPanel panel = new YamlSnippetsHelpPanel(core, link);
        panel.setSizeFull();
        core.setHelpPanel(panel);
        panel.init();
    }

    @Override
    public Icon getIcon(HelpConfiguration.HelpLink link) {
        return VaadinIcon.FILE_ADD.create();
    }
}
