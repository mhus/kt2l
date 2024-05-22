package de.mhus.kt2l.cluster;

import de.mhus.kt2l.cfg.CfgFactory;
import de.mhus.kt2l.cfg.CfgPanel;
import org.springframework.stereotype.Component;

@Component
public class ClusterCfgFactory implements CfgFactory {
    @Override
    public String handledConfigType() {
        return "clusters";
    }

    @Override
    public CfgPanel createPanel() {
        return new ClusterCfgPanel();
    }

    @Override
    public boolean isUserRelated() {
        return true;
    }

    @Override
    public boolean isProtected() {
        return false;
    }
}
