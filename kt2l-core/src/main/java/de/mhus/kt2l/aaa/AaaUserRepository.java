package de.mhus.kt2l.aaa;

import java.util.Optional;

public interface AaaUserRepository {
    AaaUser createUser(AaaUser user);

    AaaUser updateUser(AaaUser user);

    void deleteUser(String userId);

    void changePassword(String oldPassword, String newPassword);

    boolean userExists(String userId);

    Optional<AaaUser> getUserByUsername(String userId);

    Optional<AaaUser> getByEmail(String email);
}
