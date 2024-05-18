package de.mhus.kt2l.fs;

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
public class FsConfiguration extends AbstractSingleConfig {

    private List<Context> contextList;

    protected FsConfiguration() {
        super("fs", true);
    }

    public Context getFsContextForUser(String userName) {
        loadContext();
        synchronized (contextList) {
            return contextList.stream().filter(c -> c.users.size() == 0 || c.users.contains(userName)).findFirst().orElseThrow();
        }
    }

    private synchronized void loadContext() {
        if (contextList != null) return;
        contextList = new ArrayList<>();
        config().getArray("context").orElse(MTree.EMPTY_LIST).forEach(node -> {
            Context context = new Context();
            context.name = node.getString("name").get();
            context.users = Arrays.stream(ITreeNode.toStringArray(node.getArray("users").orElse(MTree.EMPTY_LIST), ITreeNode.NAMELESS_VALUE)).map(String::trim).collect(Collectors.toSet());
            context.type = node.getString("type").orElse(DirectoryDriver.NAME);
            context.root = node.getString("path").orElse("");
            context.node = node;
            contextList.add(context);
        });
    }

    @Getter
    public class Context {
        private Set<String> users;
        private String type;
        private String root;
        private ITreeNode node;
        private String name;
        private String fsContext;
    }

}
