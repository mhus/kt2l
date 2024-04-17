/**
 * This file is part of kt2l-core.
 *
 * kt2l-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * kt2l-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with kt2l-core.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l.ai;

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.mhus.commons.yaml.MYaml;
import de.mhus.commons.yaml.YElement;
import de.mhus.commons.yaml.YMap;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.core.XTab;
import de.mhus.kt2l.core.XTabListener;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import io.kubernetes.client.common.KubernetesObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

public class AiResourcePanel extends VerticalLayout implements XTabListener {

    private static final String LOADING = "Loading ... ";
    @Autowired
    private AiService ai;
    private final List<KubernetesObject> resources;
    private final ExecutionContext context;

    public AiResourcePanel(List<KubernetesObject> resources, ExecutionContext context) {
        this.resources = resources;
        this.context = context;
    }

    @Override
    public void tabInit(XTab xTab) {

        resources.forEach(resource -> {

            var text = new TextArea();
            text.setReadOnly(true);
            text.setWidthFull();
            add(text);
            text.setLabel(resource.getMetadata().getName() + ":" );
            text.setValue(LOADING);

            var menu = new ContextMenu();
            menu.setTarget(text);
            menu.addItem("Refresh", e -> {
                if (text.getValue().equals(LOADING)) return;
                text.setValue(LOADING);
                Thread.startVirtualThread(() -> {
                    processResource(resource, text);
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
                Thread.startVirtualThread(() -> {
                    processLanguage(content, text, "german");
                });
            });

            Thread.startVirtualThread(() -> {
                processResource(resource, text);
            });
        });

        setSizeFull();
    }

    private void processLanguage(String content, TextArea text, String language) {
        try {

            PromptTemplate promptTemplate = PromptTemplate.from(
                    ai.getTemplateForPrompt ("translate").orElse("Please translate to {{language}}:\n{{content}}"));
            Prompt prompt = promptTemplate.apply(Map.of("content", content, "language", language));

            final var answer = ai.generate(ai.getModelForPrompt("translate").orElse(AiService.MODEL_YI), prompt);
            context.getUi().access(() -> {
                text.removeClassName("bgcolor-yellow");
                text.setValue((answer.finishReason() != null ? answer.finishReason() + "\n" : "") + answer.content().text());
            });
        } catch (Throwable t) {
            context.getUi().access(() -> {
                text.removeClassName("bgcolor-yellow");
                text.addClassName("bgcolor-red");
                text.setValue("Error: " + t.toString());
            });
        }
    }

    private void processResource(final KubernetesObject resource, final TextArea textArea) {

        textArea.addClassName("bgcolor-yellow");
        textArea.removeClassName("bgcolor-red");
        try {
            var content = extractContent(resource);

            PromptTemplate promptTemplate = PromptTemplate.from(
                    ai.getTemplateForPrompt("resource")
                            .orElse("Do you see problems in the following kubernetes resource?\n\n{{content}}"));
            Prompt prompt = promptTemplate.apply(Map.of("content", content));

            final var answer = ai.generate(ai.getModelForPrompt("resource").orElse(AiService.MODEL_CODELLAMA), content);
            context.getUi().access(() -> {
                textArea.removeClassName("bgcolor-yellow");
                textArea.setValue((answer.finishReason() != null ? answer.finishReason() + "\n" : "") + answer.content().text());
            });
        } catch (Throwable t) {
            context.getUi().access(() -> {
                textArea.removeClassName("bgcolor-yellow");
                textArea.addClassName("bgcolor-red");
                textArea.setValue("Error: " + t.toString());
            });
        }

    }

    private String extractContent(KubernetesObject resource) {


        // get yaml
        var resContent = K8sUtil.toYaml(resource);
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

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }
}
