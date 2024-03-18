package de.mhus.kt2l.ai;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import lombok.Getter;

import java.util.Optional;

@Getter
public class AiConfiguration {
    private final String ollamaUrl;
    private final ITreeNode questions;

    public AiConfiguration(ITreeNode section) {
        ollamaUrl = section.getString("ollamaUrl").orElseGet(() -> String.format("http://%s:%d", "127.0.0.1", 11434));
        questions = section.getObject("questions").orElseGet(() -> MTree.EMPTY_MAP);
    }

    public Optional<String> getQuestion(String name) {
        return questions.getString(name);
    }
}
