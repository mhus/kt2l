package de.mhus.kt2l.aaa.oauth2;

import com.vaadin.flow.server.VaadinServletRequest;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.aaa.LoginConfiguration;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.mhus.commons.tools.MLang.tryThis;

@Component
public class AuthProvider {

    public static final String LOCAL_AUTH_PROVIDER_ID = "local";
    @Autowired
    private LoginConfiguration loginConfiguration;
    @Autowired
    private List<OAuth2AuthProvider> providerList;
    private String redirectUrl;
    private Map<String,OAuth2AuthProvider> providers;

    @PostConstruct
    public void init() {
        providers = providerList.stream()
                .collect(Collectors.toMap(OAuth2AuthProvider::getRegistrationId, p -> p));
        providers.put(LOCAL_AUTH_PROVIDER_ID, new LocalAuthProvider());
    }

    public Optional<OAuth2AuthProvider> getProvider(String providerId) {
        return Optional.ofNullable(providers.get(providerId));
    }

    public List<OAuth2AuthProvider> getAuthProviders() {
        return loginConfiguration.getOAuth2Providers()
                .stream()
                .filter(p -> providers.containsKey(p))
                .filter(p -> !p.equals(LOCAL_AUTH_PROVIDER_ID))
                .map(p -> providers.get(p))
                .toList();
    }

    public String getProividerLoginUrl(OAuth2AuthProvider provider) {
        return MString.substitute(
                provider.getLoginUrlTemplate(),
                "baseUrl", "",
                "redirectUrl", getRedirectUrl());
    }

    public String getRedirectUrl() {
        if (redirectUrl == null) {
            redirectUrl = tryThis(() -> {
                VaadinServletRequest request = VaadinServletRequest.getCurrent();
                return request.getHttpServletRequest().getHeader("origin");
                // return request.getRequestURL().toString();
            }).orElse(null);
        }
        return redirectUrl;
    }

    private class LocalAuthProvider implements OAuth2AuthProvider {

        @Override
        public String getRegistrationId() {
            return AuthProvider.LOCAL_AUTH_PROVIDER_ID;
        }

        @Override
        public String getTitle() {
            return "Local";
        }

        @Override
        public String getLoginUrlTemplate() {
            return null;
        }

        @Override
        public OAuth2UserInfo createOAuth2UserInfo(Map<String, Object> attributes) {
            return null;
        }

        @Override
        public String getImageResourcePath() {
            return null;
        }

    }
}
