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

package de.mhus.kt2l.aaa;

import de.mhus.commons.tools.MString;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.config.AbstractSingleConfig;
import de.mhus.kt2l.config.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class LoginConfiguration extends AbstractSingleConfig {

    @Autowired
    private Configuration configuration;

    private static String autoLoginPassword;

    public LoginConfiguration() {
        super("login", true);
    }

    @PostConstruct
    public void init() {
        super.init();
// is done by configuration objects intselves now
//        config().getString("protectedConfigurations").ifPresent(protectedConfigurations -> {
//            for (String protectedConfiguration : protectedConfigurations.split(",")) {
//                if (MString.isSetTrim(protectedConfiguration))
//                    configuration.addProtectedConfiguration(protectedConfiguration.trim());
//            }
//        });
    }

    public boolean isAutoLogin() {
        return config().getBoolean("autoLogin", false);
    }

    public boolean isAutoLoginLocalhostOnly() {
        return config().getBoolean("autoLoginLocalhostOnly", true);
    }

    public String getAutoLoginUser() {
        return config().getString("autoLoginUser", null);
    }

    public String getLocalAutoLoginPassword() {
        if (autoLoginPassword == null)
            autoLoginPassword = UUID.randomUUID().toString();
        return autoLoginPassword;
    }

    public boolean isLocalAutoEnabled() {
        return config().getBoolean("localAutoEnabled", true);
    }

    public List<String> getAuthProviders() {
        return MTree.getArrayValueStringList(config().getArray("authProviders").orElse(MTree.EMPTY_LIST));
    }

    public String getRedirectUrl() {
        return config().getString("redirectUrl", null);
    }
}
