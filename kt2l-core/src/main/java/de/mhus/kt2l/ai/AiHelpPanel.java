package de.mhus.kt2l.ai;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.help.HelpConfiguration;
import de.mhus.kt2l.core.MainView;
import de.mhus.kt2l.help.HelpUtil;
import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.PromptTemplate;
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
    private String lastResult;
    private MenuItem menuItemUse;

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
        menuItemUse = menuBar.addItem("Use", e -> use());
        menuItemUse.setEnabled(false);
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

    private void use() {
        HelpUtil.setResourceContent(view, lastResult);
    }

    public void sendQuestion() {
        String question = prompt.getValue();
        if (question == null || question.isBlank()) {
            return;
        }
        if (conversation == null) {
            ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(500);

            HelpUtil.getHelpResourceConnector(view).ifPresent(connector -> {
                var content = connector.getHelpContent();
                if (MString.isEmpty(content)) return;

                PromptTemplate promptTemplate = PromptTemplate.from(
                        link.getNode().getString("prompt","[resource] {{content}}"));
                var msg = promptTemplate.apply(Map.of("content", content));
                LOGGER.debug("Add initial message to conversation: {}", msg);
                chatMemory.add(msg.toSystemMessage()); //???
                text.setValue("=== Set Prompt\n");
            });

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
            lastResult = extract(answer);
            ui.access(() -> {
                text.setValue(text.getValue() + "<<< " + answer + "\n");
                prompt.setReadOnly(false);
                prompt.clear();
                menuItemUse.setEnabled(lastResult != null && HelpUtil.canSetHelpContent(view) );
            });
        });
    }

    private String extract(String answer) {
        //TODO extract resource
        return answer;
    }

    private void resetConversation() {
        conversation = null;
        text.clear();
        prompt.clear();
        prompt.focus();
        menuItemUse.setEnabled(false);
    }

    public TextField getPrompt() {
        return prompt;
    }
}
