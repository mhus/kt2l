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

@Slf4j
public abstract class AbstractUserRepository implements AaaUserRepository {

    @Override
    public AaaUser createUser(AaaUser user) {
        if (userExists(user.getUserId()))
            throw new IllegalArgumentException("User already exists: " + user.getUserId());

        AaaUser userCopy = AaaUser.copyNice(user);
        internalCreateUser(userCopy);
        return userCopy;
    }

    protected abstract void internalCreateUser(AaaUser user);

    @Override
    public AaaUser updateUser(AaaUser user) {
        // get curent user object
        var maybeCurrent = getUserByUserId(user.getUserId());
        if (maybeCurrent.isEmpty())
            throw new IllegalArgumentException("User not found: " + user.getUserId());

        // get current values
        var current = maybeCurrent.get();
        var updated = AaaUser.update(current, user);
        // do not update if nothing changed
        if (current.equals(updated))
            return updated;

        internalUpdateUser(updated);
        return updated;
    }

    protected abstract void internalUpdateUser(AaaUser updatedUser);

}
