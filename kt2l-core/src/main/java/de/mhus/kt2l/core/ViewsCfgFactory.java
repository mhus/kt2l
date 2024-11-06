package de.mhus.kt2l.core;

import de.mhus.kt2l.cfg.CfgFactory;
import de.mhus.kt2l.cfg.CfgPanel;
import org.springframework.stereotype.Service;

@Service
public class ViewsCfgFactory implements CfgFactory {
    @Override
    public String handledConfigType() {
        return "views";
    }

    @Override
    public CfgPanel createPanel() {
        return new ViewsCfgPanel();
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
