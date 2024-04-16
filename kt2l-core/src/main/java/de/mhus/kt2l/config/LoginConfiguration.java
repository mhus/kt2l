package de.mhus.kt2l.config;

import de.mhus.commons.tree.ITreeNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.UUID;

@Component
public class LoginConfiguration extends AbstractSingleConfig {

    @Autowired
    private Configuration configuration;

    private static String autoLoginPassword;

    public LoginConfiguration() {
        super("login", true);
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
}