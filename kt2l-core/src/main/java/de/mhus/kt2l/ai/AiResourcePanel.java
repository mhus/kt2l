/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.mhus.kt2l.ai;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.mhus.commons.yaml.MYaml;
import de.mhus.commons.yaml.YElement;
import de.mhus.commons.yaml.YMap;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.core.SecurityContext;
import de.mhus.kt2l.help.HelpResourceConnector;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.util.ResourceSelector;
import de.mhus.kt2l.ui.UiUtil;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import io.kubernetes.client.common.KubernetesObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Slf4j
public class AiResourcePanel extends VerticalLayout implements DeskTabListener, HelpResourceConnector {


    private MenuItem itemModeResources;
    private MenuItem itemModeCumulative;
    private MenuItem itemModeText;

    @Override
    public String getHelpContent() {
        return question.getValue();
    }

    @Override
    public void setHelpContent(String content) {
        question.setValue(content);
    }

    @Override
    public int getHelpCursorPos() {
        return -1;
    }

    private enum PROCESS_MODE {RESOURCES, CUMULATIVE, TEXT};
    private PROCESS_MODE processMode;
    private String language = null;
    private static final String LOADING = "Loading ... ";
    @Autowired
    private AiService ai;
    @Autowired
    private AiConfiguration config;
    private final ResourceSelector<KubernetesObject> selector;
    private final Core core;
    private VerticalLayout results;
    private TextArea question;
    private String codingModel;

    public AiResourcePanel(List<KubernetesObject> resources, Core core) {
        this.selector = new ResourceSelector<>(resources, true);
        this.core = core;
    }

    @Override
    public void tabInit(DeskTab deskTab) {

        setMargin(false);
        setPadding(false);
        setSizeFull();

        var menuBar = new MenuBar();
        selector.injectMenu(menuBar);
        var itemMode = menuBar.addItem("Mode");
        var modeMenu = itemMode.getSubMenu();
        itemModeResources = modeMenu.addItem("Resources", e -> setMode(PROCESS_MODE.RESOURCES));
        itemModeCumulative = modeMenu.addItem("Cumulative", e -> setMode(PROCESS_MODE.CUMULATIVE));
        itemModeText = modeMenu.addItem("Text", e -> setMode(PROCESS_MODE.TEXT));
        itemModeResources.setCheckable(true);
        itemModeCumulative.setCheckable(true);
        itemModeText.setCheckable(true);
        itemModeResources.setChecked(true);

        menuBar.addItem(VaadinIcon.PLAY.create(), e -> {
            results.removeAll();
            answerQuestion(question.getValue());
        }).add("Ask");
        add(menuBar);

        var resultScroller = new Scroller();
        resultScroller.setSizeFull();
        add(resultScroller);

        results = new VerticalLayout();
        results.setSizeFull();
        results.setMargin(false);
        results.setPadding(false);
        resultScroller.setContent(results);
        var resultLabel = new TextArea();
        resultLabel.setSizeFull();
        resultLabel.setReadOnly(true);
        resultLabel.setValue("Results");
        results.add(resultLabel);

        question = new TextArea("Question (Ctrl+Enter to send)");
        question.setWidthFull();
        question.setHeight("150px");
        question.setValueChangeMode(ValueChangeMode.EAGER);
        question.addKeyPressListener(
                e -> {
                    if (e.getKey().equals(Key.ENTER) && e.getModifiers().contains(KeyModifier.CONTROL)) {
                        results.removeAll();
                        answerQuestion(question.getValue());
                    }
                });

        add(question);

        codingModel =  ai.getModelForPrompt("resource").orElse(AiService.AUTO_CODING);
        processMode = PROCESS_MODE.RESOURCES;
        language = config.getDefaultLanguage();
        var template = ai.getTemplateForPrompt("resource")
                .orElse("Do you see problems in the following kubernetes resource?\n\n{{content}}");
        question.setValue(template);
        question.focus();

    }

    private void answerQuestion(String question) {

        while (question.startsWith("/")) {
            var pos = question.indexOf("\n");
            if (pos < 0) {
                processCommand(question.trim());
                return;
            }
            var cmd = question.substring(0, pos).trim();
            question = question.substring(pos + 1).trim();
            processCommand(cmd);
        }

        if (question.startsWith("#")) {
            question = question.substring(1);
        } else
        if (!question.contains("{{content}}")) {
            question = "{{content}}\n\n" + question;
        }
        LOGGER.debug("Question: {}", question);

        this.question.setEnabled(false);

        if (processMode == PROCESS_MODE.RESOURCES) {
            processResources(question);
        } else if (processMode == PROCESS_MODE.CUMULATIVE) {
            processCumulative(question);
        } else if (processMode == PROCESS_MODE.TEXT) {
            processText(question);
        }

    }

    private void processText(String question) {

        var text = new TextArea();
        text.setReadOnly(true);
        text.setWidthFull();
        results.add(text);
        text.setLabel("Text Answer");
        text.setValue(LOADING);

        createContextMenu(text);

        var sc = SecurityContext.create();
        Thread.startVirtualThread(() -> {
            try (var sce = sc.enter()) {

                PromptTemplate promptTemplate = PromptTemplate.from(question);
                Prompt prompt = promptTemplate.apply(Map.of("content", ""));

                processResource(text, prompt);
            }
        });

    }

    private void processCumulative(String question) {
        StringBuilder content = new StringBuilder();
        selector.getResources().forEach(resource -> {
            content.append("```\n").append(extractContent(resource)).append("\n```\n\n");
        });

        var text = new TextArea();
        text.setReadOnly(true);
        text.setWidthFull();
        results.add(text);
        text.setLabel("Cumulative Answer");
        text.setValue(LOADING);

        createContextMenu(text);

        var sc = SecurityContext.create();
        Thread.startVirtualThread(() -> {
            try (var sce = sc.enter()) {

                PromptTemplate promptTemplate = PromptTemplate.from(question);
                Prompt prompt = promptTemplate.apply(Map.of("content", content.toString()));

                processResource(text, prompt);
            }
        });

    }

    private void processResources(String question) {
        selector.getResources().forEach(resource -> {

            var text = new TextArea();
            text.setReadOnly(true);
            text.setWidthFull();
            results.add(text);
            text.setLabel(resource.getMetadata().getName() + ":" );
            text.setValue(LOADING);

            createContextMenu(text);

            var sc = SecurityContext.create();
            Thread.startVirtualThread(() -> {
                try (var sce = sc.enter()) {

                    var content = extractContent(resource);

                    PromptTemplate promptTemplate = PromptTemplate.from(question);
                    Prompt prompt = promptTemplate.apply(Map.of("content", content));

                    processResource(text, prompt);
                }
            });
        });

    }

    private void processCommand(String question) {

        if (question.startsWith("/mode ")) {

            var mode = question.substring(6).trim();
            if (mode.equalsIgnoreCase("resources")) {
                setMode(PROCESS_MODE.RESOURCES);
            } else if (mode.equalsIgnoreCase("cumulative")) {
                setMode(PROCESS_MODE.CUMULATIVE);
            } else if (mode.equalsIgnoreCase("direct")) {
                setMode(PROCESS_MODE.TEXT);
            }
            UiUtil.showSuccessNotification("Mode changed to " + processMode);

        } else if (question.startsWith("/model ")) {

            codingModel = question.substring(7).trim();
            UiUtil.showSuccessNotification("Model changed to " + codingModel);

        } else if (question.startsWith("/language ")) {

            language = question.substring(10).trim();
            UiUtil.showSuccessNotification("Language changed to " + language);

        } else {
            UiUtil.showErrorNotification("Unknown command: " + question);
        }
    }

    private void setMode(PROCESS_MODE mode) {
        processMode = mode;
        itemModeResources.setChecked(mode != PROCESS_MODE.RESOURCES);
        itemModeCumulative.setChecked(mode != PROCESS_MODE.CUMULATIVE);
        itemModeText.setChecked(mode != PROCESS_MODE.TEXT);
    }

    private void createContextMenu(TextArea text) {
        var menu = new ContextMenu();
        menu.setTarget(text);
        menu.addItem("To " + language, e -> {
            if (text.getValue().equals(LOADING)) return;
            var content = text.getValue();
            text.setValue(LOADING);
            text.addClassName("bgcolor-yellow");
            text.removeClassName("bgcolor-red");
            var sc = SecurityContext.create();
            Thread.startVirtualThread(() -> {
                try (var sce = sc.enter()) {
                    processLanguage(content, text, language);
                }
            });
        });
    }

    private void processLanguage(String content, TextArea text, String language) {
        try {

            PromptTemplate promptTemplate = PromptTemplate.from(
                    ai.getTemplateForPrompt ("translate").orElse("Please translate to {{language}}:\n{{content}}"));
            Prompt prompt = promptTemplate.apply(Map.of("content", content, "language", language));

            final var answer = ai.generate(ai.getModelForPrompt("translate").orElse(AiService.AUTO_TRANSLATE), prompt);
            core.ui().access(() -> {
                text.removeClassName("bgcolor-yellow");
                text.setValue((answer.finishReason() != null ? answer.finishReason() + "\n" : "") + answer.content().text());
            });
        } catch (Throwable t) {
            core.ui().access(() -> {
                text.removeClassName("bgcolor-yellow");
                text.addClassName("bgcolor-red");
                text.setValue("Error: " + t.toString());
            });
        }
    }

    private void processResource(final TextArea textArea, Prompt prompt) {

        core.ui().access(() -> {
            textArea.addClassName("bgcolor-yellow");
            textArea.removeClassName("bgcolor-red");
        });
        try {

            final var answer = ai.generate(codingModel, prompt);
            core.ui().access(() -> {
                textArea.removeClassName("bgcolor-yellow");
                textArea.setValue((answer.finishReason() != null ? answer.finishReason() + "\n" : "") + answer.content().text());
                this.question.setEnabled(true);
            });
        } catch (Throwable t) {
            core.ui().access(() -> {
                textArea.removeClassName("bgcolor-yellow");
                textArea.addClassName("bgcolor-red");
                textArea.setValue("Error: " + t.toString());
                this.question.setEnabled(true);
            });
        }

    }

    private String extractContent(KubernetesObject resource) {


        // get yaml
        var resContent = K8sUtil.toYamlString(resource);
        YElement yDocument = MYaml.loadFromString(resContent);

        YMap yMetadata = yDocument.asMap().getMap("metadata");
        YMap yManagedFields = null;
        if (yMetadata != null) {
            yManagedFields = yMetadata.getMap("managedFields");
        }
        if (yManagedFields != null) {
            MYaml.toString(yManagedFields);
            ((Map<String, Object>) yMetadata.getObject()).remove("managedFields");
        }
        YMap yStatus = yDocument.asMap().getMap("status");
        if (yStatus != null) {
            ((Map<String, Object>) yDocument.asMap().getObject()).remove("status");
        }

        // recreate without status and managedFields
        resContent = MYaml.toString(yDocument);
        return resContent;
    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabUnselected() {

    }

    @Override
    public void tabDestroyed() {

    }

    @Override
    public void tabRefresh(long counter) {

    }

}
