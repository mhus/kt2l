package de.mhus.kt2l.core;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import de.mhus.kt2l.config.LoginConfiguration;
import de.mhus.kt2l.config.UsersConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
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

        http.headers(new Customizer<HeadersConfigurer<HttpSecurity>>() {
            @Override
            public void customize(HeadersConfigurer<HttpSecurity> httpSecurityHeadersConfigurer) {
                httpSecurityHeadersConfigurer.frameOptions(new Customizer<HeadersConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>.FrameOptionsConfig>() {
                    @Override
                    public void customize(HeadersConfigurer<HttpSecurity>.FrameOptionsConfig frameOptionsConfig) {
                        frameOptionsConfig.disable();
                    }
                });
            }
        });
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

    /**
     * Demo UserDetailsManager which only provides two hardcoded
     * in memory users and their roles.
     * NOTE: This shouldn't be used in real world applications.
     */
    @Bean
    public UserDetailsManager userDetailsService() {
        final var userDetails = new ArrayList<UserDetails>();
        usersConfig.getUsers().forEach(u -> {
            LOGGER.info("Add user {} with roles {}", u.name(), u.roles());
            var password = u.password();
            if (loginConfig.isAutoLogin() && u.name().equals(loginConfig.getAutoLoginUser())) {
                var p = loginConfig.getLocalAutoLoginPassword();
                password = passwordEncoder().encode(p);
                LOGGER.info("Set autologin password for user {} to {}", u.name(), p);
            } else
            if (password == null || "{generate}".equals(password)) {
                var p = UUID.randomUUID().toString();
                password = passwordEncoder().encode(p);
                LOGGER.info("Set login password for user {} to {}", u.name(), p);
            } else
            if (password.startsWith("{env}")) {
                password = passwordEncoder().encode(System.getenv(password.substring(5)));
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
                new DelegatingPasswordEncoder("bcrypt",encoders);
        return passwordEncoder;
    }
}