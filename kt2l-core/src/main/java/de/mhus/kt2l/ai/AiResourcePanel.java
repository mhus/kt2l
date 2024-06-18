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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.mhus.commons.yaml.MYaml;
import de.mhus.commons.yaml.YElement;
import de.mhus.commons.yaml.YMap;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.core.SecurityContext;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.util.ResourceSelector;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import io.kubernetes.client.common.KubernetesObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Slf4j
public class AiResourcePanel extends VerticalLayout implements DeskTabListener {

    private static final String LOADING = "Loading ... ";
    @Autowired
    private AiService ai;
    private final ResourceSelector<KubernetesObject> selector;
    private final Core core;
    private VerticalLayout results;
    private TextArea question;

    public AiResourcePanel(List<KubernetesObject> resources, Core core) {
        this.selector = new ResourceSelector<>(resources, true);
        this.core = core;
    }

    @Override
    public void tabInit(DeskTab deskTab) {

        setMargin(false);
        setPadding(false);

        var resultScroller = new Scroller();
        resultScroller.setSizeFull();
        add(resultScroller);

        var menuBar = new MenuBar();
        selector.injectMenu(menuBar);
        menuBar.addItem(VaadinIcon.FAST_FORWARD.create(), e -> {
            results.removeAll();
            answerQuestion(question.getValue());
        }).setText("Ask");


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

        var template = ai.getTemplateForPrompt("resource")
                .orElse("Do you see problems in the following kubernetes resource?\n\n{{content}}");
        question.setValue(template);
        question.focus();

    }

    private void answerQuestion(String question) {

        if (question.startsWith("#")) {
            question = question.substring(1);
        } else
        if (!question.contains("{{content}}")) {
            question = "{{content}}\n\n" + question;
        }
        final var questionFinal = question;
        LOGGER.debug("Question: {}", question);

        this.question.setEnabled(false);

        selector.getResources().forEach(resource -> {

            var text = new TextArea();
            text.setReadOnly(true);
            text.setWidthFull();
            results.add(text);
            text.setLabel(resource.getMetadata().getName() + ":" );
            text.setValue(LOADING);

            var menu = new ContextMenu();
            menu.setTarget(text);
            menu.addItem("Refresh", e -> {
                if (text.getValue().equals(LOADING)) return;
                text.setValue(LOADING);
                var sc = SecurityContext.create();
                Thread.startVirtualThread(() -> {
                    try (var sce = sc.enter()) {
                        processResource(resource, text, questionFinal);
                    }
                });
            });
            menu.addItem("Clear", e -> {
                if (text.getValue().equals(LOADING)) return;
                text.setValue("");
            });
            menu.addItem("To german", e -> {
                if (text.getValue().equals(LOADING)) return;
                var content = text.getValue();
                text.setValue(LOADING);
                text.addClassName("bgcolor-yellow");
                text.removeClassName("bgcolor-red");
                var sc = SecurityContext.create();
                Thread.startVirtualThread(() -> {
                    try (var sce = sc.enter()) {
                        processLanguage(content, text, "german");
                    }
                });
            });

            var sc = SecurityContext.create();
            Thread.startVirtualThread(() -> {
                try (var sce = sc.enter()) {
                    processResource(resource, text, questionFinal);
                }
            });
        });

        setSizeFull();
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

    private void processResource(final KubernetesObject resource, final TextArea textArea, String question) {

        core.ui().access(() -> {
            textArea.addClassName("bgcolor-yellow");
            textArea.removeClassName("bgcolor-red");
        });
        try {
            var content = extractContent(resource);

            PromptTemplate promptTemplate = PromptTemplate.from(question);
            Prompt prompt = promptTemplate.apply(Map.of("content", content));

            final var answer = ai.generate(ai.getModelForPrompt("resource").orElse(AiService.AUTO_CODING), prompt);
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
