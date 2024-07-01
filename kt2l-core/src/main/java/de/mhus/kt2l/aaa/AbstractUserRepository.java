package de.mhus.kt2l.aaa;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public abstract class AbstractUserRepository implements AaaUserRepository {

    @Override
    public AaaUser updateUser(AaaUser user) {
        // get curent user object
        var maybeCurrent = getUserByUsername(user.getUsername());
        if (maybeCurrent.isEmpty())
            throw new IllegalArgumentException("User not found: " + user.getUsername());

        // get current values
        var current = maybeCurrent.get();
        var roles = current.getRoles();
        var password = current.getEncodedPassword();
        var email = current.getEmail();
        var imageUrl = current.getImageUrl();

        // update only if not null
        if (user.getRoles() != null)
            roles = user.getRoles();
        // not allowed to change password only for admins
        if (user.getEncodedPassword() != null && !password.equals(user.getEncodedPassword())) {
            if (!SecurityUtils.hasPrincipalRoles(Set.of("admin")))
                throw new IllegalArgumentException("Not allowed to change password");
            password = user.getEncodedPassword();
        }
        if (user.getEmail() != null)
            email = user.getEmail();
        if (user.getImageUrl() != null)
            imageUrl = user.getImageUrl();

        // create new user object
        var newUser = AaaUser.builder()
                .username(user.getUsername())
                .encodedPassword(password)
                .roles(roles)
                .email(email)
                .imageUrl(imageUrl)
                .build();

        internalUpdateUser(newUser);
        return newUser;
    }

    protected abstract void internalUpdateUser(AaaUser newUser);

}
