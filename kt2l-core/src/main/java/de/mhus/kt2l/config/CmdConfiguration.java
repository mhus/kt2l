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
package de.mhus.kt2l.config;

import de.mhus.commons.tools.MCollection;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tools.MSystem;
import de.mhus.commons.tree.MProperties;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.core.UiUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
public class CmdConfiguration extends AbstractUserRelatedConfig {

    protected CmdConfiguration() {
        super("cmd-" + MSystem.getOS().name().toLowerCase() );
    }

    public void openWebBrowser(String url) {
        var conf = config().getArray("webbrowser").orElse(null);
        if (conf != null) {
            MProperties vars = new MProperties();
            vars.put("url", url);
            execute("webbrowser", vars);
        } else
            MSystem.openBrowserUrl(url);
    }

    public void openFileBrowser(String path) {
        var conf = config().getArray("filebrowser").orElse(null);
        if (conf != null) {
            MProperties vars = new MProperties();
            vars.put("path", path);
            execute("filebrowser", vars);
        } else
            MSystem.openFileBrowser(path);
    }

    public MSystem.ScriptResult execute(String name, MProperties vars) {
        var conf = config().getArray(name).orElse(MTree.EMPTY_LIST);
        final var osCmd = MTree.getArrayValueStringList(conf);
        final String[] osCmdArray = osCmd.toArray(new String[0]);
        MCollection.replaceAll(osCmdArray, v -> MString.substitute(v, vars, v) );
        LOGGER.info("Execute: {}", Arrays.toString(osCmdArray));

        try {
            var res = MSystem.execute(osCmdArray);
            LOGGER.info("Result: {}", res);
            if (res.getRc() != 0)
                UiUtil.showErrorNotification("Failed to start Terminal");
            return res;
        } catch (Exception e) {
            UiUtil.showErrorNotification("Unexpected Error", e);
            LOGGER.error("Error execute {}", Arrays.toString(osCmdArray), e);
            return null;
        }

    }
}
