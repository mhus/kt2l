package de.mhus.kt2l.aaa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.UUID;

@Component
@Slf4j
public class UsersConfigurationRepository extends InMemoryUserRepository implements AaaUserRepository {

    @Autowired
    private LoginConfiguration loginConfig;
    @Autowired
    private UsersConfiguration usersConfig;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        usersConfig.getUsers().forEach(u -> {
            LOGGER.info("Add user {} with roles {}", u.name(), u.roles());
            var password = u.password();
            if (loginConfig.isAutoLogin() && u.name().equals(loginConfig.getAutoLoginUser())) {
                var p = loginConfig.getLocalAutoLoginPassword();
                password = passwordEncoder.encode(p);
                if (SecurityUtils.isUnsecure())
                    LOGGER.info("Set autologin password for user {} to {}", u.name(), p);
            } else
            if (password == null || "{generate}".equals(password)) {
                var p = UUID.randomUUID().toString();
                password = passwordEncoder.encode(p);
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
            createUser(AaaUser.builder().username(u.name())
                    .encodedPassword(password)
                    .roles(u.roles())
                    .build());
        });
    }

}
