package de.mhus.kt2l.ai;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.generic.ExecutionContext;
import de.mhus.kt2l.generic.IResourceProvider;
import de.mhus.kt2l.generic.ResourceAction;
import de.mhus.kt2l.ui.PanelService;
import io.kubernetes.client.common.KubernetesObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class AiAction implements ResourceAction  {

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandleResourceType(String resourceType) {
        return true;
    }

    @Override
    public boolean canHandleResource(String resourceType, Set<? extends KubernetesObject> selected) {
        return selected.size() > 0;
    }

    @Override
    public void execute(ExecutionContext context) {

        List<KubernetesObject> resources = new LinkedList<>();
        for (var selected : context.getSelected()) {
            resources.add(selected);
        }

        if (resources.size() == 0) return;

        // process
        var name = resources.getFirst().getMetadata().getName();
        panelService.addPanel(
                context.getSelectedTab(),
                context.getClusterConfiguration().name() + ":" + context.getResourceType() + ":" + name + ":ai",
                name,
                false,
                VaadinIcon.ACADEMY_CAP.create(),
                () -> new AiResourcePanel(resources, context)
                ).select();
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
