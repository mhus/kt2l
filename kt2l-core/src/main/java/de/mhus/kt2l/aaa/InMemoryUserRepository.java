package de.mhus.kt2l.aaa;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class InMemoryUserRepository extends AbstractUserRepository {

    private final Map<String, AaaUser> users = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void createUser(AaaUser user) {
        LOGGER.info("Create user: {}", user);
        users.put(user.getUsername(), user);
    }


    @Override
    public void deleteUser(String username) {
        LOGGER.info("Delete user: {}", username);
        users.remove(username);
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        var username = SecurityContext.lookupUserName();
        LOGGER.info("Change password for user: {}", username);
        var user = users.get(username);
        if (user == null)
            throw new IllegalArgumentException("User not found: " + username);
        user.setEncodedPassword(oldPassword, newPassword);
    }

    @Override
    public boolean userExists(String username) {
        return users.containsKey(username);
    }

    @Override
    public Optional<AaaUser> getUserByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    @Override
    public Optional<AaaUser> getByEmail(String email) {
        return users.values().stream().filter(u -> email.equals(u.getEmail())).findFirst();
    }

    @Override
    protected void internalUpdateUser(AaaUser newUser) {
        LOGGER.info("Update user: {}", newUser);
        users.put(newUser.getUsername(), newUser);
    }
}
