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
package de.mhus.kt2l.cfg;

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

        panelService.addGlobalCfgPanel(core, globalFactories, configDir, globalDir).select();
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
        panelService.addUserCfgPanel(core, factories, configDir, localUserDir, localDir, globalDir).select();

    }


}
