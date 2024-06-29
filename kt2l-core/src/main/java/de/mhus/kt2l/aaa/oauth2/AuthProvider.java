package de.mhus.kt2l.aaa.oauth2;

import com.vaadin.flow.server.VaadinServletRequest;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.aaa.LoginConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.mhus.commons.tools.MLang.tryThis;

@Component
public class AuthProvider {

    @Autowired
    private LoginConfiguration loginConfiguration;
    private String redirectUrl;

    public Optional<Provider> getProvider(String registrationId) {
        return tryThis(() -> new Provider(SUPPORTED_AUTH_PROVIDERS.valueOf(registrationId))).toOptional();
    }

    private enum SUPPORTED_AUTH_PROVIDERS {
        local("Local", null, null, null),
        facebook("Facebook","${baseUrl}/oauth2/authorize/facebook?redirect_uri=${redirectUrl}", "/images/facebook-logo.svg", FacebookOAuth2UserInfo.class),
        google("Google", "${baseUrl}/oauth2/authorize/google?redirect_uri=${redirectUrl}", "/images/google-logo.svg", GoogleOAuth2UserInfo.class),
        github("Github", "${baseUrl}/oauth2/authorize/github?redirect_uri=${redirectUrl}", "/images/github-logo.svg", GithubOAuth2UserInfo.class);

        @Getter
        private final String title;
        @Getter
        private final String authUrl;
        @Getter
        private final String iconPath;
        @Getter
        private final Class<? extends OAuth2UserInfo> userInfoClass;

        private SUPPORTED_AUTH_PROVIDERS(String title, String authUrl, String iconPath, Class<? extends OAuth2UserInfo> userInfoClass ) {
            this.title = title;
            this.authUrl = authUrl;
            this.iconPath = iconPath;
            this.userInfoClass = userInfoClass;
        }
    }

    public List<Provider> getAuthProviders() {
        return loginConfiguration.getAuthProviders()
                .stream()
                .filter(p -> tryThis(() -> SUPPORTED_AUTH_PROVIDERS.valueOf(p)).isSuccess())
                .filter(p -> SUPPORTED_AUTH_PROVIDERS.valueOf(p) != SUPPORTED_AUTH_PROVIDERS.local)
                .map(p -> new Provider(SUPPORTED_AUTH_PROVIDERS.valueOf(p)))
                .toList();
    }

    public class Provider {
        private final SUPPORTED_AUTH_PROVIDERS provider;

        public Provider(SUPPORTED_AUTH_PROVIDERS provider) {
            this.provider = provider;
        }

        public String getTitle() {
            return provider.getTitle();
        }

        public String getLink() {
            return MString.substitute(
                    provider.getAuthUrl(),
                    "baseUrl", "",
                    "redirectUrl", getRedirectUrl());
        }

        public OAuth2UserInfo createOAuth2UserInfo(Map<String, Object> attributes) {
            try {
                return provider.userInfoClass.getConstructor(Map.class).newInstance(attributes);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getRedirectUrl() {
        if (redirectUrl == null) {
            redirectUrl = loginConfiguration.getRedirectUrl();
            if (redirectUrl == null) {
                VaadinServletRequest request = VaadinServletRequest.getCurrent();
                if (request != null) {
                    var httpRequest = request.getHttpServletRequest();
                    if (httpRequest != null) {
                        redirectUrl = httpRequest.getHeader("origin");
                    }
                }
            }
        }
        return redirectUrl;
    }

}
