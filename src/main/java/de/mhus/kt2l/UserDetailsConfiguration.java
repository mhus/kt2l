package de.mhus.kt2l;

import de.mhus.commons.node.ITreeNode;
import de.mhus.commons.node.MNode;
import de.mhus.commons.node.TreeNodeList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserDetailsConfiguration {
    private final ITreeNode config;

    public UserDetailsConfiguration(ITreeNode config) {
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
                        MNode.getArrayValueStringList(user.getArray("roles").orElse(new TreeNodeList("", null)))
                ));
            });
        return users;
    }


    public static record User(String name, String password, Collection<String> roles) {
    }
}
