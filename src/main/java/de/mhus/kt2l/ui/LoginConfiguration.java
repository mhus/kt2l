package de.mhus.kt2l.ui;

import de.mhus.commons.tree.ITreeNode;

import java.util.UUID;

public class LoginConfiguration {
    private final ITreeNode config;
    private static String autoLoginPassword;

    public LoginConfiguration(ITreeNode config) {
        this.config = config;
    }

    public boolean isAutoLogin() {
        return config.getBoolean("autoLogin", false);
    }

    public boolean isAutoLoginLocalhostOnly() {
        return config.getBoolean("autoLoginLocalhostOnly", true);
    }

    public String getAutoLoginUser() {
        return config.getString("autoLoginUser", null);
    }

    public String getLocalAutoLoginPassword() {
        if (autoLoginPassword == null)
            autoLoginPassword = UUID.randomUUID().toString();
        return autoLoginPassword;
    }
}
