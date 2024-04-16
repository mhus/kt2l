package de.mhus.kt2l.ai;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.config.AbstractUserRelatedConfig;
import de.mhus.kt2l.config.Configuration;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Getter
@Component
public class AiConfiguration extends AbstractUserRelatedConfig {

    public AiConfiguration() {
        super("ai");
    }

    public String getOllamaUrl() {
        return config().getString("ollamaUrl").orElseGet(() -> String.format("http://%s:%d", "127.0.0.1", 11434));
    }
    public Optional<String> getTemplate(String name) {
        return config().getObject("prompts").orElseGet(() -> MTree.EMPTY_MAP).getObject(name).orElse(MTree.EMPTY_MAP).getString("template");
    }

    public Optional<String> getModel(String name) {
        return config().getObject("prompts").orElseGet(() -> MTree.EMPTY_MAP).getObject(name).orElse(MTree.EMPTY_MAP).getString("model");
    }

    public boolean isEnabled() {
        return config().getBoolean("enabled", false);
    }

}
