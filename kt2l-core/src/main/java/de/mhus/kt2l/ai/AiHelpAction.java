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

package de.mhus.kt2l.ai;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.help.HelpAction;
import de.mhus.kt2l.help.HelpConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiHelpAction implements HelpAction {

    @Autowired
    private AiConfiguration aiConfiguration;

    @Autowired
    private AiService aiService;

    @Override
    public boolean canHandle(HelpConfiguration.HelpLink link) {
        return "ai".equals(link.getAction()) && aiConfiguration.isEnabled();
    }

    @Override
    public void execute(Core core, HelpConfiguration.HelpLink link) {
        AiHelpPanel panel = new AiHelpPanel(core, link, aiService);
        panel.setSizeFull();
        core.setHelpPanel(panel);
        panel.getPrompt().focus();
    }

    @Override
    public Icon getIcon(HelpConfiguration.HelpLink link) {
        return VaadinIcon.CROSSHAIRS.create();
    }
}
