package de.mhus.kt2l.ai;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.commons.yaml.MYaml;
import de.mhus.commons.yaml.YElement;
import de.mhus.commons.yaml.YMap;
import de.mhus.kt2l.generic.ExecutionContext;
import de.mhus.kt2l.generic.IResourceProvider;
import de.mhus.kt2l.generic.ResourceAction;
import de.mhus.kt2l.generic.ResourceDetailsPanel;
import de.mhus.kt2l.generic.TextPanel;
import de.mhus.kt2l.k8s.K8sUtil;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.util.Yaml;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class AiAction implements ResourceAction  {

    @Override
    public boolean canHandleResourceType(String resourceType) {
        return true;
    }

    @Override
    public boolean canHandleResource(String resourceType, Set<?> selected) {
        return selected.size() > 0;
    }

    @Override
    public void execute(ExecutionContext context) {

        String managedFieldsContent = null;
        String statusContent = null;

        // find resource
        var selected = context.getSelected().iterator().next();
        if (selected instanceof IResourceProvider) selected = ((IResourceProvider)selected).getResource();

        String namespace = "";
        String name = selected.toString();

        if (selected instanceof Map) {
            var metadata = (Map) ((Map) selected).get("metadata");
            namespace = (String) metadata.get("namespace");
            name = (String) metadata.get("name");
        } else
        if (selected instanceof KubernetesObject) {
            var metadata = ((KubernetesObject) selected).getMetadata();
            namespace = metadata.getNamespace();
            name = metadata.getName();
        }
        final var resource = (KubernetesObject)selected;

        // get yaml
        var types = K8sUtil.getResourceTypes(context.getApi());
        var resType = K8sUtil.findResource(context.getResourceType(), types);

        var resContent = Yaml.dump(resource);
        YElement yDocument = MYaml.loadFromString(resContent);

        YMap yMetadata = yDocument.asMap().getMap("metadata");
        YMap yManagedFields = null;
        if (yMetadata != null) {
            yManagedFields = yMetadata.getMap("managedFields");
        }
        if (yManagedFields != null) {
            managedFieldsContent = MYaml.toString(yManagedFields);
            ((Map<String, Object>) yMetadata.getObject()).remove("managedFields");
        }
        YMap yStatus = yDocument.asMap().getMap("status");
        if (yStatus != null) {
            ((Map<String, Object>)yDocument.asMap().getObject()).remove("status");
            statusContent = MYaml.toString(yStatus);
        }

        // init AI
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(baseUrl())
                .modelName("mistral")
//                .format("json")
                .build();

        var msg = "Do you see problems in the following kubernetes resource?\n\n" + resContent;
        String answer = model.generate(msg);

        // show answer
        context.getMainView().getTabBar().addTab(
                context.getClusterConfiguration().name() + ":" + context.getResourceType() + ":" + name + ":ai",
                name,
                true,
                false,
                VaadinIcon.FILE_TEXT_O.create(),
                () ->
                        new TextPanel(answer)
                        ).setColor(context.getClusterConfiguration().color()).select().setParentTab(context.getSelectedTab());



    }

    @Override
    public String getTitle() {
        return "AI";
    }

    @Override
    public String getMenuBarPath() {
        return "";
    }

    @Override
    public String getShortcutKey() {
        return "a";
    }

    @Override
    public String getPopupPath() {
        return "";
    }

    @Override
    public String getDescription() {
        return "Analyse with AI";
    }

    static String baseUrl() {
//        return String.format("http://%s:%d", ollama.getHost(), ollama.getFirstMappedPort());
        return String.format("http://%s:%d", "127.0.0.1", 11434);
    }

}
