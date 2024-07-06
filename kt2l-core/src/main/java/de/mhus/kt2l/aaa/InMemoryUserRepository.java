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
