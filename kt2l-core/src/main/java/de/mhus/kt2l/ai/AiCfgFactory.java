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

import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cfg.CfgFactory;
import de.mhus.kt2l.cfg.CfgPanel;
import org.springframework.stereotype.Component;

import static de.mhus.kt2l.aaa.UsersConfiguration.ROLE.SETTINGS;

@Component
@WithRole(SETTINGS)
public class AiCfgFactory implements CfgFactory  {
    @Override
    public String handledConfigType() {
        return "ai";
    }

    @Override
    public CfgPanel createPanel() {
        return new AiCfgPanel();
    }

    @Override
    public boolean isUserRelated() {
        return false;
    }

    @Override
    public boolean isProtected() {
        return false;
    }
}
