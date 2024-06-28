package de.mhus.kt2l.aaa;

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
        userRepository.createUser(toAaaUser(user));
    }

    private AaaUser toAaaUser(UserDetails user) {
        return  AaaUser.builder()
        .username(user.getUsername())
        .encodedPassword(user.getPassword())
        .roles(user.getAuthorities().stream().map(a -> a.getAuthority()).toList())
        .build();
    }

    @Override
    public void updateUser(UserDetails user) {
        userRepository.updateUser(toAaaUser(user));
    }

    @Override
    public void deleteUser(String username) {
        userRepository.deleteUser(username);
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        userRepository.changePassword(oldPassword, newPassword);
    }

    @Override
    public boolean userExists(String username) {
        return userRepository.userExists(username);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var maybeUser = userRepository.getUserByUsername(username);
        if (maybeUser.isEmpty())
            throw new UsernameNotFoundException("User not found: " + username);
        var user = maybeUser.get();
        return User.withUsername(user.getUsername())
                .password(user.getEncodedPassword())
                .roles(user.getRoles().toArray(new String[0])).build();
    }
}
