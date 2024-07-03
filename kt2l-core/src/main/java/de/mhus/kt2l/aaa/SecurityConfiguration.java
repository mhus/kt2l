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

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableWebSecurity
@Configuration
@Slf4j
public class SecurityConfiguration
        extends VaadinWebSecurity {

    @Autowired
    private LoginConfiguration loginConfig;
    @Autowired
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Delegating the responsibility of general configurations
        // of http security to the super class. It's configuring
        // the followings: Vaadin's CSRF protection by ignoring
        // framework's internal requests, default request cache,
        // ignoring public views annotated with @AnonymousAllowed,
        // restricting access to other views/endpoints, and enabling
        // NavigationAccessControl authorization.
        // You can add any possible extra configurations of your own
        // here (the following is just an example):

        // http.rememberMe().alwaysRemember(false);

        // Configure your static resources with public access before calling
        // super.configure(HttpSecurity) as it adds final anyRequest matcher
        http.authorizeHttpRequests(auth -> auth.requestMatchers(new AntPathRequestMatcher("/public/**"))
                .permitAll());

        if (loginConfig.isOAuth2Enabled()) {
            LOGGER.info("OAuth2 enabled");
            http.oauth2Login(oauth2Login ->
                    oauth2Login.loginPage("/login")
                            .permitAll()
                            .defaultSuccessUrl("/", true)
                            .failureUrl("/public/sso-failed.html")
                            .userInfoEndpoint(userInfoEndpoint ->
                                    userInfoEndpoint.userService(customOAuth2UserService)
                            )
            );
        }

        http.headers(httpSecurityHeadersConfigurer -> httpSecurityHeadersConfigurer.frameOptions(frameOptionsConfig -> frameOptionsConfig.disable()));
        super.configure(http);

        // This is important to register your login view to the
        // navigation access control mechanism:
        setLoginView(http, LoginView.class);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        // Customize your WebSecurity configuration.
        super.configure(web);
    }

}