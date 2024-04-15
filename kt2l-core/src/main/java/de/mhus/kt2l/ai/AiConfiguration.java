package de.mhus.kt2l.ai;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.config.Configuration;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Getter
@Component
public class AiConfiguration {

    @Autowired
    private Configuration configuration;
    private ITreeNode config;
    private boolean enabled;

    private String ollamaUrl;
    private ITreeNode prompts;

    @PostConstruct
    private void init() {
        this.config = configuration.getSection("ai");
        enabled = config.getBoolean("enabled", false);
        ollamaUrl = config.getString("ollamaUrl").orElseGet(() -> String.format("http://%s:%d", "127.0.0.1", 11434));
        prompts = config.getObject("prompts").orElseGet(() -> MTree.EMPTY_MAP);
    }
    
    public Optional<String> getTemplate(String name) {
        return prompts.getObject(name).orElse(MTree.EMPTY_MAP).getString("template");
    }

    public Optional<String> getModel(String name) {
        return prompts.getObject(name).orElse(MTree.EMPTY_MAP).getString("model");
    }

    public boolean isEnabled() {
        return enabled;
    }

}
