package de.mhus.kt2l.config;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.commons.tree.TreeNodeList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UsersConfiguration {

    public enum ROLE {
        READ,
        WRITE,
        SETTINGS,
        LOCAL,
        ADMIN
    }

    private final ITreeNode config;

    public UsersConfiguration(ITreeNode config) {
        this.config = config;
    }

    public List<User> getUsers() {
        final var users = new ArrayList<User>();
        final var userConfig = config.getArray("users");
        if (userConfig.isPresent())
            userConfig.get().forEach(user -> {
                users.add(new User(
                        user.getString("name").get(),
                        user.getString("password").get(),
                        MTree.getArrayValueStringList(user.getArray("roles").orElse(new TreeNodeList("", null)))
                ));
            });
        return users;
    }


    public static record User(String name, String password, Collection<String> roles) {
    }
}
