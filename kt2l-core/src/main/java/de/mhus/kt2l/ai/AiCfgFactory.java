package de.mhus.kt2l.ai;

import de.mhus.kt2l.cfg.CfgFactory;
import de.mhus.kt2l.cfg.CfgPanel;
import org.springframework.stereotype.Component;

@Component
public class AiCfgFactory implements CfgFactory  {
    @Override
    public String handledConfigType() {
        return "ai";
    }

    @Override
    public CfgPanel createPanel() {
        return new AiCfgPanel();
    }

    @Override
    public boolean isUserRelated() {
        return false;
    }

    @Override
    public boolean isProtected() {
        return false;
    }
}
