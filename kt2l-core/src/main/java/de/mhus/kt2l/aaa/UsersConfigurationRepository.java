package de.mhus.kt2l.aaa;

import de.mhus.commons.errors.AccessDeniedException;
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
            LOGGER.info("Add user {} with roles {}", u.id(), u.roles());
            var password = u.password();
            if (loginConfig.isAutoLogin() && u.id().equals(loginConfig.getAutoLoginUser())) {
                var p = loginConfig.getLocalAutoLoginPassword();
                password = passwordEncoder.encode(p);
                if (SecurityUtils.isUnsecure())
                    LOGGER.info("Set autologin password for user {} to {}", u.id(), p);
            } else if (password == null || "{generate}".equals(password)) {
                var p = UUID.randomUUID().toString();
                password = passwordEncoder.encode(p);
                if (SecurityUtils.isUnsecure())
                    LOGGER.info("Set login password for user {} to {}", u.id(), p);
            } else if (password.startsWith("{env}")) {
//                password = passwordEncoder().encode(System.getenv(password.substring(5)));
                final var envValue = System.getenv(password.substring(5)).trim();
                if (envValue == null || envValue.length() == 0)
                    throw new IllegalArgumentException("Environment variable not found: " + password.substring(5));
                if (envValue.startsWith("{"))
                    password = envValue;
                else
                    password = "{noop}" + envValue;
            }
            super.createUser(AaaUser.builder()
                    .userId(u.id())
                    .displayName(u.name())
                    .encodedPassword(password)
                    .roles(u.roles())
                    .build());
        });
    }

    @Override
    public AaaUser createUser(AaaUser user) {
        if (!usersConfig.allowCreateUsers())
            throw new AccessDeniedException("Create users not allowed");
        return super.createUser(user);
    }

    @Override
    public AaaUser updateUser(AaaUser user) {
        if (!usersConfig.allowUpdateUsers())
            throw new AccessDeniedException("Update users not allowed");
        return super.updateUser(user);
    }

    @Override
    public void deleteUser(String userId) {
        if (!usersConfig.allowDeleteUsers())
            throw new AccessDeniedException("Delete users not allowed");
        super.deleteUser(userId);
    }

}