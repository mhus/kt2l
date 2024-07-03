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
        var maybeCurrent = getUserByUsername(user.getUserId());
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
