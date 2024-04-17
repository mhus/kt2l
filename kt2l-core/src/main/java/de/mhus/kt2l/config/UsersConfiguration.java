/**
 * This file is part of kt2l-core.
 *
 * kt2l-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * kt2l-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with kt2l-core.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l.config;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.commons.tree.TreeNodeList;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class UsersConfiguration extends AbstractSingleConfig {

    public enum ROLE {
        READ,
        WRITE,
        SETTINGS,
        LOCAL,
        ADMIN
    }

    public UsersConfiguration() {
        super("users");
    }

    public List<User> getUsers() {
        final var users = new ArrayList<User>();
        final var userConfig = config().getArray("users");
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
