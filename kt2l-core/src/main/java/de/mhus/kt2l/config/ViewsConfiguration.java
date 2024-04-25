package de.mhus.kt2l.config;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ViewsConfiguration extends AbstractUserRelatedConfig {

    protected ViewsConfiguration() {
        super("views");
    }

    public ITreeNode getConfig(String viewName) {
        return config().getObject(viewName).orElse(MTree.EMPTY_MAP);
    }

}
