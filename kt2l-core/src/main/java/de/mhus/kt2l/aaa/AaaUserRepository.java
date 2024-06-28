package de.mhus.kt2l.aaa;

import java.util.Optional;

public interface AaaUserRepository {
    void createUser(AaaUser user);

    AaaUser updateUser(AaaUser user);

    void deleteUser(String username);

    void changePassword(String oldPassword, String newPassword);

    boolean userExists(String username);

    Optional<AaaUser> getUserByUsername(String username);

    Optional<AaaUser> getByEmail(String email);
}
