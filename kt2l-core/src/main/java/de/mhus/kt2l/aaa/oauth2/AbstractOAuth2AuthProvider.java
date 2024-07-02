package de.mhus.kt2l.aaa.oauth2;

import de.mhus.kt2l.aaa.LoginConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashSet;

public abstract class AbstractOAuth2AuthProvider implements OAuth2AuthProvider {

    protected LoginConfiguration.OAuthProvider config;

    @Autowired
    protected LoginConfiguration loginConfiguration;

    @PostConstruct
    public void init() {
        config = loginConfiguration.getOAuth2Provider(getRegistrationId()).orElse(null);
    }

    @Override
    public String getLoginUrlTemplate() {
        return "/oauth2/authorization/" + getRegistrationId();
    }

    @Override
    public String getImageResourcePath() {
        return "/images/" + getRegistrationId() + "-logo.svg";
    }


    protected Collection<String> mapRoles(Collection<String> roles) {
        var result = new HashSet<String>();
        roles.forEach(role -> result.addAll(config.getRoleMapping(role)));
        return result;
    }

    protected Collection<String> getRoles(Collection<? extends GrantedAuthority> authorities) {
        return new HashSet<>(authorities.stream().map(GrantedAuthority::getAuthority).toList());
    }


}
