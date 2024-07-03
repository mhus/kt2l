package de.mhus.kt2l.aaa;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class InMemoryUserRepository extends AbstractUserRepository {

    private final Map<String, AaaUser> users = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void deleteUser(String userId) {
        LOGGER.info("Delete user: {}", userId);
        users.remove(userId);
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        var userId = SecurityContext.lookupUserId();
        LOGGER.info("Change password for user: {}", userId);
        var user = users.get(userId);
        if (user == null)
            throw new IllegalArgumentException("User not found: " + userId);
        user.setEncodedPassword(oldPassword, newPassword);
    }

    @Override
    public boolean userExists(String userId) {
        return users.containsKey(userId);
    }

    @Override
    public Optional<AaaUser> getUserByUsername(String userId) {
        return Optional.ofNullable(users.get(userId));
    }

    @Override
    public Optional<AaaUser> getByEmail(String email) {
        return users.values().stream().filter(u -> email.equals(u.getEmail())).findFirst();
    }

    @Override
    protected void internalCreateUser(AaaUser user) {
        LOGGER.info("Create user: {}", user);
        users.put(user.getUserId(), user);
    }

    @Override
    protected void internalUpdateUser(AaaUser updatedUser) {
        LOGGER.info("Update user: {}", updatedUser);
        users.put(updatedUser.getUserId(), updatedUser);
    }
}
