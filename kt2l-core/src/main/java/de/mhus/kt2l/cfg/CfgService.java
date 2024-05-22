package de.mhus.kt2l.cfg;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.commons.tools.MFile;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.core.SecurityContext;
import de.mhus.kt2l.core.SecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CfgService {

    @Autowired
    private Configuration configuration;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private List<CfgFactory> cfgFactories;
    @Autowired
    private PanelService panelService;

    private Boolean canWriteGlobalCfg = null;
    private Map<String, Boolean> canWriteUserCfg = new HashMap<>();

    public boolean isUserCfgEnabled() {

        if (!securityService.hasRole(UsersConfiguration.ROLE.SETTINGS.name())) return false;
        var userName = SecurityContext.lookupUserName();
        return canWriteUserCfg.computeIfAbsent(userName, n -> {
            var userConfigDir = configuration.getUserConfigurationDirectory(n);
            var testFile = new File(userConfigDir,"test.txt");
            if (!MFile.touch(testFile)) return false;
            testFile.delete();
            return true;
        });
    }

    public boolean isGlobalCfgEnabled() {

        if (!securityService.hasRole(UsersConfiguration.ROLE.ADMIN.name())) return false;
        if (canWriteGlobalCfg == null) {
            var globalConfigDir = configuration.getLocalConfigurationDirectory();
            var testFile = new File(globalConfigDir,"test.txt");
            if (!MFile.touch(testFile)) canWriteGlobalCfg = false;
            else {
                testFile.delete();
                canWriteGlobalCfg = true;
            }
        }
        return canWriteGlobalCfg;
    }

    public void showGlobalCfg(Core core) {

        // collect all factories
        List<CfgFactory> globalFactories = cfgFactories;
//        List<CfgFactory> userFactories = cfgFactories.stream().filter(f -> f.isUserRelated()).toList();
        File configDir = configuration.getLocalConfigurationDirectory();
        File globalDir = configuration.getGlobalConfigurationDirectory();

        panelService.addPanel(core, null, "global-cfg", "Global Settings", true, VaadinIcon.COGS.create(),
                () -> new GlobalCfgPanel(core, true, globalFactories, configDir, globalDir)).setHelpContext("global_cfg").select();
    }

    public void showUserCfg(Core core) {

        // collect all factories
        List<CfgFactory> factories = cfgFactories.stream().filter(f -> f.isUserRelated() && !f.isProtected()).toList();

        // collect all factories
//        List<CfgFactory> userFactories = cfgFactories.stream().filter(f -> f.isUserRelated()).toList();
        var userName = SecurityContext.lookupUserName();
        File configDir = configuration.getUserConfigurationDirectory(userName);

        File localUserDir = configuration.getLocalUserConfigurationDirectory(userName);
        File localDir = configuration.getLocalConfigurationDirectory();
        File globalDir = configuration.getGlobalConfigurationDirectory();

        panelService.addPanel(core, null, "user-cfg", "User Settings", true, VaadinIcon.COG.create(),
                () -> new GlobalCfgPanel(core, false, factories, configDir, localUserDir, localDir, globalDir)).setHelpContext("user_cfg").select();

    }


}
