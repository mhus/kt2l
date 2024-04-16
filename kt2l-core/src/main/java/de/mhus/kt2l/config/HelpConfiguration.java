package de.mhus.kt2l.config;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.commons.tree.TreeNodeList;
import de.mhus.kt2l.help.HelpAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HelpConfiguration extends AbstractUserRelatedConfig {

    public String getWindowWidth() {
        return config().getString("windowWidth", "300px");
    }

    public HelpConfiguration() {
        super("help");
    }

    public HelpContext getContext(String name) {
        return new HelpContext(
                config().getObject("contexts").orElse(MTree.EMPTY_MAP)
                        .getArray(name).orElse(MTree.EMPTY_LIST));
    }


    public static class HelpContext {

        private final TreeNodeList context;
        private final Map<String, HelpLink> links = new HashMap<>();
        private final List<String> order = new ArrayList<>();

        private HelpContext(TreeNodeList context) {
            this.context = context;
            context.forEach(o -> {
                var link = new HelpLink(o);
                links.put(link.getName(), link);
                order.add(link.getName());
            });
        }

        public List<HelpLink> getLinks() {
            return order.stream().map(name -> links.get(name)).toList();
        }

    }

    public static class HelpLink {

        private final ITreeNode link;
        private HelpAction helpAction;

        private HelpLink(ITreeNode link) {
            this.link = link;
        }

        public String getName() {
            return link.getString("name").get();
        }

        public String getAction() {
            return link.getString("action").get();
        }

        public ITreeNode getNode() {
            return link;
        }

        public boolean isEnabled() {
            return link.getBoolean("enabled", true);
        }

        public boolean isDefault() {
            return link.getBoolean("default", true);
        }

        public void setHelpAction(HelpAction helpAction) {
            this.helpAction = helpAction;
        }

        public HelpAction getHelpAction() {
            return helpAction;
        }
    }
}
