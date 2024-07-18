/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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
    private AaaUserRepositoryService userRepository;

    @Override
    public void createUser(UserDetails user) {
        userRepository.getRepository().createUser(toAaaUser(user, true));
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
        userRepository.getRepository().updateUser(toAaaUser(user, false));
    }

    @Override
    public void deleteUser(String userId) {
        userRepository.getRepository().deleteUser(userId);
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        userRepository.getRepository().changePassword(oldPassword, newPassword);
    }

    @Override
    public boolean userExists(String userId) {
        return userRepository.getRepository().userExists(userId);
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        var maybeUser = userRepository.getRepository().getUserByUserId(userId);
        if (maybeUser.isEmpty())
            throw new UsernameNotFoundException("User not found: " + userId);
        var user = maybeUser.get();
        return User.withUsername(user.getUserId())
                .password(user.getEncodedPassword())
                .roles(user.getRoles().toArray(new String[0])).build();
    }
}
