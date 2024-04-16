package de.mhus.kt2l.ai;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import de.mhus.commons.tools.MLang;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.config.HelpConfiguration;
import de.mhus.kt2l.core.MainView;
import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class AiHelpPanel extends VerticalLayout {
    private final MainView view;
    private final HelpConfiguration.HelpLink link;
    private final AiService aiService;
    private ChatLanguageModel model;
    private TextArea text;
    private TextField prompt;
    private ConversationalChain conversation;

    public AiHelpPanel(MainView view, HelpConfiguration.HelpLink link, AiService aiService) {
        this.view = view;
        this.link = link;
        this.aiService = aiService;
        model = aiService.getModel(link.getNode().getString("model").orElse(AiService.MODEL_LLAMA2));
        initUi();
    }

    private void initUi() {
        setPadding(false);
        setMargin(false);
        MenuBar menuBar = new MenuBar();
        menuBar.addItem("Reset", e -> resetConversation());
        add(menuBar);
        text = new TextArea();
        text.setReadOnly(true);
        text.setSizeFull();
        add(text);
        prompt = new TextField("Please enter your question here");
        prompt.setWidthFull();
        prompt.addKeyPressListener(Key.ENTER, e -> sendQuestion());
        add(prompt);
    }

    public void sendQuestion() {
        String question = prompt.getValue();
        if (question == null || question.isBlank()) {
            return;
        }
        if (conversation == null) {
            ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(500);
            MLang.tryThis(() -> {
                var selectedPanel = view.getTabBar().getSelectedTab().getPanel();
                if (selectedPanel instanceof AiConnector connector) {

                    var content = connector.getAiChatContent();
                    if (MString.isEmpty(content)) return;

                    PromptTemplate promptTemplate = PromptTemplate.from(
                            link.getNode().getString("prompt","[resource] {{content}}"));
                    var msg = promptTemplate.apply(Map.of("content", content));
                    chatMemory.add(msg.toSystemMessage()); //???
                    text.setValue("=== Set Prompt\n");
                }
            }).onError(e -> LOGGER.error("Error getting selected panel", e));
            conversation = ConversationalChain.builder()
                    .chatLanguageModel(model)
                    .chatMemory(chatMemory)
                    .build();
        }
        text.setValue(text.getValue() + "\n>>> " + question + "\n");
        prompt.setValue("Processing ...");
        prompt.setReadOnly(true);
        final var ui = UI.getCurrent();
        Thread.startVirtualThread(() -> {
            String answer = conversation.execute(question);
            ui.access(() -> {
                text.setValue(text.getValue() + "<<< " + answer + "\n");
                prompt.setReadOnly(false);
                prompt.clear();
            });
        });
    }

    private void resetConversation() {
        conversation = null;
        text.clear();
        prompt.clear();
        prompt.focus();
    }

    public TextField getPrompt() {
        return prompt;
    }
}
