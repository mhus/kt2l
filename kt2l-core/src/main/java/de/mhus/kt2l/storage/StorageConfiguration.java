package de.mhus.kt2l.storage;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.config.AbstractSingleConfig;
import lombok.Getter;
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
            bucket.users = Arrays.stream(ITreeNode.toStringArray(node.getArray("users").orElse(MTree.EMPTY_LIST), ITreeNode.NAMELESS_VALUE)).map(String::trim).collect(Collectors.toSet());
            bucket.type = node.getString("type").orElse(DirectoryDriver.NAME);
            bucket.root = node.getString("path").orElse("");
            bucket.node = node;
            bucketList.add(bucket);
        });
    }

    public boolean isEnabled() {
        return config().getBoolean("enabled").orElse(true);
    }

    @Getter
    public class Bucket {
        private Set<String> users;
        private String type;
        private String root;
        private ITreeNode node;
        private String name;
        private String fsContext;
    }

}
