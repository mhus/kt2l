package de.mhus.kt2l.ai;

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.mhus.commons.tools.MString;
import de.mhus.commons.yaml.MYaml;
import de.mhus.commons.yaml.YElement;
import de.mhus.commons.yaml.YMap;
import de.mhus.kt2l.generic.ExecutionContext;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.ui.XTab;
import de.mhus.kt2l.ui.XTabListener;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.util.Yaml;
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
            content = MString.compileAndExecute(
                    ai.getQuestion("translate").orElse("Please translate to ${language}:\n${content}"),
                    "language", language,
                    "content", content
            );

            final var answer = ai.generate(AiService.MODEL_YI, content, language);
            context.getUi().access(() -> {
                text.setValue((answer.finishReason() != null ? answer.finishReason() + "\n" : "") + answer.content().text());
            });
        } catch (Throwable t) {
            context.getUi().access(() -> {
                text.setValue("Error: " + t.toString());
            });
        }
    }

    private void processResource(final KubernetesObject resource, final TextArea textArea) {

        try {
            var content = extractContent(resource);

            content = MString.compileAndExecute(
                    ai.getQuestion("resource").orElse("Do you see problems in the following kubernetes resource?\n\n${content}"),
                    "content", content);

            final var answer = ai.generate(AiService.MODEL_CODELLAMA, content);
            context.getUi().access(() -> {
                textArea.setValue((answer.finishReason() != null ? answer.finishReason() + "\n" : "") + answer.content().text());
            });
        } catch (Throwable t) {
            context.getUi().access(() -> {
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
