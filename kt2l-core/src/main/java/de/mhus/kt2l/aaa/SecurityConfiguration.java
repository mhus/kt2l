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
import de.mhus.kt2l.config.LoginConfiguration;
import de.mhus.kt2l.config.UsersConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EnableWebSecurity
@Configuration
@Slf4j
public class SecurityConfiguration
        extends VaadinWebSecurity {

    @Autowired
    private LoginConfiguration loginConfig;
    @Autowired
    private UsersConfiguration usersConfig;
//    @Autowired
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService;

    private boolean oauth2Enabled = false;

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

        http.headers(httpSecurityHeadersConfigurer -> httpSecurityHeadersConfigurer.frameOptions(frameOptionsConfig -> frameOptionsConfig.disable()));
        super.configure(http);

        if (oauth2Enabled) {
            http.oauth2Login(oauth2Login ->
                oauth2Login.loginPage("/login")
                    .permitAll()
                    .defaultSuccessUrl("/", true)
                    .userInfoEndpoint(userInfoEndpoint ->
                            userInfoEndpoint.userService(customOAuth2UserService)
                    )
            );
        }
        // This is important to register your login view to the
        // navigation access control mechanism:
        setLoginView(http, LoginView.class);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        // Customize your WebSecurity configuration.
        super.configure(web);
    }

    @Bean
    public UserDetailsManager userDetailsService() {
        final var userDetails = new ArrayList<UserDetails>();
        usersConfig.getUsers().forEach(u -> {
            LOGGER.info("Add user {} with roles {}", u.name(), u.roles());
            var password = u.password();
            if (loginConfig.isAutoLogin() && u.name().equals(loginConfig.getAutoLoginUser())) {
                var p = loginConfig.getLocalAutoLoginPassword();
                password = passwordEncoder().encode(p);
                if (SecurityUtils.isUnsecure())
                    LOGGER.info("Set autologin password for user {} to {}", u.name(), p);
            } else
            if (password == null || "{generate}".equals(password)) {
                var p = UUID.randomUUID().toString();
                password = passwordEncoder().encode(p);
                if (SecurityUtils.isUnsecure())
                    LOGGER.info("Set login password for user {} to {}", u.name(), p);
            } else
            if (password.startsWith("{env}")) {
//                password = passwordEncoder().encode(System.getenv(password.substring(5)));
                final var envValue = System.getenv(password.substring(5)).trim();
                if (envValue == null || envValue.length() == 0)
                    throw new IllegalArgumentException("Environment variable not found: " + password.substring(5));
                if (envValue.startsWith("{"))
                    password = envValue;
                else
                    password = "{noop}" + envValue;
            }
            userDetails.add(User.withUsername(u.name())
                    .password(password)
                    .roles(u.roles().toArray(new String[0]))
                    .build());
        });
        return new InMemoryUserDetailsManager(userDetails);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        Map encoders = new HashMap();
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        encoders.put("noop", NoOpPasswordEncoder.getInstance());
        encoders.put("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_5());
        encoders.put("pbkdf2@SpringSecurity_v5_8", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("scrypt", SCryptPasswordEncoder.defaultsForSpringSecurity_v4_1());
        encoders.put("scrypt@SpringSecurity_v5_8", SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("argon2", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_2());
        encoders.put("argon2@SpringSecurity_v5_8", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("sha256", new StandardPasswordEncoder());

        PasswordEncoder passwordEncoder =
                new DelegatingPasswordEncoder("sha256",encoders);
        return passwordEncoder;
    }
}