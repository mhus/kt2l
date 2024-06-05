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

import de.mhus.commons.tools.MString;
import de.mhus.kt2l.core.Core;

import static de.mhus.commons.tools.MString.isSetTrim;

public class SnippetsHelpPanel extends AbstractGitSnippetsHelpPanel {

    public SnippetsHelpPanel(Core core, HelpConfiguration.HelpLink link) {
        super( link.getNode().getString("type","yaml"), core, link);
    }

    @Override
    protected void transferContent(String snippet) {
        var current = HelpUtil.getHelpResourceConnector(core).get().getHelpContent();
        if (current == null) return;

        if (MString.isEmptyTrim(current)) {
            HelpUtil.setResourceContent(core, snippet);
            return;
        }
        var pos = HelpUtil.getHelpResourceConnector(core).get().getHelpCursorPos();
        if (getCodeType().equals("yaml")) {
            if (pos < 0) {
                HelpUtil.setResourceContent(core, current + "\n---\n" + snippet);
                return;
            }
            var before = current.substring(0, pos);
            var after = current.substring(pos);

            HelpUtil.setResourceContent(core, (isSetTrim(before) ? before + "\n---\n" : "") + snippet + (isSetTrim(after) ? "\n---\n" + after : ""));
        } else {
            if (pos <= 0) {
                HelpUtil.setResourceContent(core, current + "\n" + snippet);
            } else {
                var before = current.substring(0, pos);
                var after = current.substring(pos);
                HelpUtil.setResourceContent(core, before + "\n" + snippet + (isSetTrim(after) ? "\n" + after : ""));
            }
        }
    }
}
