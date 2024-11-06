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

import de.mhus.kt2l.cfg.CPanelVerticalLayout;
import de.mhus.kt2l.form.YBoolean;
import de.mhus.kt2l.form.YText;
import org.springframework.beans.factory.annotation.Configurable;

@Configurable
public class ViewsCfgPanel extends CPanelVerticalLayout {

    @Override
    public String getTitle() {
        return "Views";
    }

    @Override
    public void initUi() {
        add(new YBoolean()
                .path("core/darkMode")
                .label("Dark Mode as default")
                .defaultValue(true));
        add(new YBoolean()
                .path("core/autoDarkMode")
                .label("Automatic Dark Mode")
                .defaultValue(true));
        add(new YText()
                .path("localBash/bash")
                .label("Local bash")
                .defaultValue("/bin/bash"));
        add(new YText()
                .path("localBash/argument")
                .label("Local bash argument")
                .defaultValue("-i"));

    }
}
