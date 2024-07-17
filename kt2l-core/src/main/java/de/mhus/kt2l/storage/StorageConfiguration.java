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
package de.mhus.kt2l.storage;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.config.AbstractSingleConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class StorageConfiguration extends AbstractSingleConfig {

    private List<Bucket> bucketList;

    protected StorageConfiguration() {
        super("storage", true);
    }

    public Bucket getBucketForUser(String userName) {
        loadContext();
        synchronized (bucketList) {
            return bucketList.stream().filter(c -> c.users.size() == 0 || c.users.contains(userName)).findFirst().orElseThrow();
        }
    }

    private synchronized void loadContext() {
        if (bucketList != null) return;
        bucketList = new ArrayList<>();
        config().getArray("buckets").orElse(MTree.EMPTY_LIST).forEach(node -> {
            Bucket bucket = new Bucket();
            bucket.name = node.getString("name").get();
            bucket.users = Arrays.stream(MTree.toStringArray(node.getArray("users").orElse(MTree.EMPTY_LIST), ITreeNode.NAMELESS_VALUE)).map(String::trim).collect(Collectors.toSet());
            bucket.type = node.getString("type").orElse(DirectoryDriver.NAME);
            bucket.root = node.getString("root").orElse("");
            bucket.node = node;
            bucketList.add(bucket);
        });
    }

    public boolean isEnabled() {
        return config().getBoolean("enabled").orElse(true);
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Bucket {
        private Set<String> users;
        private String type;
        private String root;
        private ITreeNode node;
        private String name;
        private String fsContext;
    }

}
