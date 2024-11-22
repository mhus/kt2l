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

import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.config.AbstractSingleConfig;
import de.mhus.kt2l.config.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static de.mhus.commons.tools.MString.isEmpty;
import static de.mhus.commons.tools.MString.isSet;

@Component
public class LoginConfiguration extends AbstractSingleConfig {

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

    public boolean isLocalAuthEnabled() {
        return config().getBoolean("localAuthEnabled", true);
    }

    public boolean isOAuth2Enabled() {
        return config().getBoolean("oauth2Enabled", false);
    }

    public List<OAuthProvider> getOAuth2Providers() {
        return config().getArray("oauth2Providers").orElse(MTree.EMPTY_LIST).stream().map(e -> new OAuthProvider(e)).toList();
    }

    public Optional<OAuthProvider> getOAuth2Provider(String id) {
        return config().getArray("oauth2Providers").orElse(MTree.EMPTY_LIST).stream().filter(e -> e.getString("id").orElse("").equals(id)).findFirst().map(e -> new OAuthProvider(e));
    }

    public List<OAuthAccepted> getOAuth2Accept() {
        return config().getArray("oauth2Accept").orElse(MTree.EMPTY_LIST).stream().map(e -> new OAuthAccepted(e)).toList();
    }

    public String getRedirectUrl() {
        return config().getString("redirectUrl", null);
    }

    public String getLoginText() {
        return config().getString("loginText", null);
    }

    public boolean isShowLoginHeader() {
        return config().getBoolean("showLoginHeader", true);
    }

    public String getUserRepositoryClass() {
        return config().getString("userRepositoryClass", null);
    }

    public static class OAuthAccepted {

        private final ITreeNode item;

        private OAuthAccepted(ITreeNode item) {
            this.item = item;
        }

        public String getPattern() {
            return item.getString("pattern", null);
        }

        public String getProvider() {
            return item.getString("provider", null);
        }

        public boolean accept(AaaUser user) {
            return
                    (isEmpty(getProvider()) || getProvider().equals(user.getProvider()))
                    &&
                    isSet(user.getEmail()) && user.getEmail().matches(getPattern())
                    &&
                    acceptRoles(user);
        }

        private boolean acceptRoles(AaaUser user) {
            var acceptRoles = getAcceptRoles();
            if (acceptRoles.isEmpty())
                return true;
            for (String userRole : user.getRoles())
                for (String role : acceptRoles)
                    if (userRole.matches(role))
                        return true;
            return false;
        }


        public List<String> getDefaultRoles() {
            return MTree.getArrayValueStringList(item.getArray("defaultRoles").orElse(MTree.EMPTY_LIST));
        }

        public List<String> getAcceptRoles() {
            return MTree.getArrayValueStringList(item.getArray("acceptRoles").orElse(MTree.EMPTY_LIST));
        }

        public String getUserConfigPreset() {
            return item.getString("userConfigPreset", null);
        }
    }

    public static class OAuthProvider {

        private final ITreeNode item;

        private OAuthProvider(ITreeNode item) {
            this.item = item;
        }

        public String getId() {
            return item.getString("id", null);
        }

        public List<String> getRoleMapping(String role) {
            return MTree.getArrayValueStringList(item.getObject("roleMapping").orElse(MTree.EMPTY_MAP).getArray(role).orElse(MTree.EMPTY_LIST));
        }
    }
}
