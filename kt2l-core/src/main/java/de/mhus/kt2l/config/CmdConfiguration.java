package de.mhus.kt2l.config;

import de.mhus.commons.tools.MCollection;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tools.MSystem;
import de.mhus.commons.tree.MProperties;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.core.UiUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
