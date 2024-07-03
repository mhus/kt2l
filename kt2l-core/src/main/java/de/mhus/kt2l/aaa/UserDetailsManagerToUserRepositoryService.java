package de.mhus.kt2l.aaa;

import de.mhus.kt2l.aaa.oauth2.AuthProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Component;

@Component
public class UserDetailsManagerToUserRepositoryService implements UserDetailsManager {

    @Autowired
    private AaaUserRepository userRepository;

    @Override
    public void createUser(UserDetails user) {
        userRepository.createUser(toAaaUser(user, true));
    }

    public static AaaUser toAaaUser(UserDetails user, boolean create) {
        var builder =  AaaUser.builder();
        builder.userId(user.getUsername());
        if (create) {
            builder.displayName(user.getUsername());
        }
        builder.provider(AuthProvider.LOCAL_AUTH_PROVIDER_ID);
        builder.providerId(user.getUsername());
        builder.encodedPassword(user.getPassword());
        builder.roles(
                user.getAuthorities()
                        .stream()
                        .map(a -> a.getAuthority())
                        .filter(a -> a.startsWith("ROLE_"))
                        .map(a -> a.substring(5))
                        .toList()
        );
        return builder.build();
    }

    @Override
    public void updateUser(UserDetails user) {
        userRepository.updateUser(toAaaUser(user, false));
    }

    @Override
    public void deleteUser(String userId) {
        userRepository.deleteUser(userId);
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        userRepository.changePassword(oldPassword, newPassword);
    }

    @Override
    public boolean userExists(String userId) {
        return userRepository.userExists(userId);
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        var maybeUser = userRepository.getUserByUsername(userId);
        if (maybeUser.isEmpty())
            throw new UsernameNotFoundException("User not found: " + userId);
        var user = maybeUser.get();
        return User.withUsername(user.getUserId())
                .password(user.getEncodedPassword())
                .roles(user.getRoles().toArray(new String[0])).build();
    }
}
