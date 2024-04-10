package de.mhus.kt2l.config;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HelpConfiguration {

    @Autowired
    private Configuration configuration;

    private ITreeNode config;

    @PostConstruct
    private void init() {
        this.config = configuration.getSection("help");
    }

    public String getWindowWidth() {
        return config.getString("windowWidth", "300px");
    }

    public HelpContext getContext(String name) {
        return new HelpContext(
                config.getObject("contexts").orElse(MTree.EMPTY_MAP)
                        .getObject(name).orElse(MTree.EMPTY_MAP));
    }


    public static class HelpContext {

        private final ITreeNode context;
        private final Map<String, HelpLink> links = new HashMap<>();
        private final List<String> order = new ArrayList<>();

        private HelpContext(ITreeNode context) {
            this.context = context;
            context.getObjects().forEach(o -> {
                var link = new HelpLink(o);
                links.put(link.getName(), link);
                order.add(link.getName());
            } );
        }

    }

    public static class HelpLink {

        private final ITreeNode link;

        private HelpLink(ITreeNode link) {
            this.link = link;
        }

        public String getName() {
            return link.getString("name").get();
        }

        public String getHref() {
            if (link.isProperty("document")) {
                return "/public/docs/" + link.getString("document").get() + ".html";
            }
            if (link.isProperty("href")) {
                return link.getString("href").get();
            }
            return "/public/default.html";
        }
    }
}
