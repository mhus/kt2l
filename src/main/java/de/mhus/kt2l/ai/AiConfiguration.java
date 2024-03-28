package de.mhus.kt2l.ai;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import lombok.Getter;

import java.util.Optional;

@Getter
public class AiConfiguration {
    private final String ollamaUrl;
    private final ITreeNode prompts;

    public AiConfiguration(ITreeNode section) {
        ollamaUrl = section.getString("ollamaUrl").orElseGet(() -> String.format("http://%s:%d", "127.0.0.1", 11434));
        prompts = section.getObject("prompts").orElseGet(() -> MTree.EMPTY_MAP);
    }

    public Optional<String> getTemplate(String name) {
        return prompts.getObject(name).orElse(MTree.EMPTY_MAP).getString("template");
    }

    public Optional<String> getModel(String name) {
        return prompts.getObject(name).orElse(MTree.EMPTY_MAP).getString("model");
    }

}
