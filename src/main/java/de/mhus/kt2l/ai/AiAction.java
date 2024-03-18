package de.mhus.kt2l.ai;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.generic.ExecutionContext;
import de.mhus.kt2l.generic.IResourceProvider;
import de.mhus.kt2l.generic.ResourceAction;
import io.kubernetes.client.common.KubernetesObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
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

        List<KubernetesObject> resources = new LinkedList<>();
        for (var selected : context.getSelected()) {
            try {
                if (selected instanceof IResourceProvider) selected = ((IResourceProvider) selected).getResource();

                String namespace = "";
                String name = selected.toString();

                if (selected instanceof Map) {
                    var metadata = (Map) ((Map) selected).get("metadata");
                    namespace = (String) metadata.get("namespace");
                    name = (String) metadata.get("name");
                } else if (selected instanceof KubernetesObject) {
                    var metadata = ((KubernetesObject) selected).getMetadata();
                    namespace = metadata.getNamespace();
                    name = metadata.getName();
                }
                final var resource = (KubernetesObject) selected;

                resources.add(resource);
            } catch (Exception e) {
                LOGGER.warn("canHandleResource {}", selected, e);
            }
        }

        if (resources.size() == 0) return;

        // process
        var name = resources.getFirst().getMetadata().getName();
        context.getMainView().getTabBar().addTab(
                context.getClusterConfiguration().name() + ":" + context.getResourceType() + ":" + name + ":ai",
                name,
                true,
                false,
                VaadinIcon.ACADEMY_CAP.create(),
                () ->
                        new AiResourcePanel(resources, context)
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


}
