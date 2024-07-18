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

import java.util.Optional;

public interface AaaUserRepository {
    AaaUser createUser(AaaUser user);

    AaaUser updateUser(AaaUser user);

    void deleteUser(String userId);

    void changePassword(String oldPassword, String newPassword);

    boolean userExists(String userId);

    Optional<AaaUser> getUserByUserId(String userId);

    Optional<AaaUser> getByEmail(String email);
}
