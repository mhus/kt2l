package de.mhus.kt2l.ai;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.config.UsersConfiguration.ROLE;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.ui.PanelService;
import de.mhus.kt2l.ui.WithRole;
import io.kubernetes.client.common.KubernetesObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@WithRole(ROLE.READ)
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
                ).setHelpContext("ai").select();
    }

    @Override
    public String getTitle() {
        return "AI;icon=" + VaadinIcon.CROSSHAIRS;
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.TOOLS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.TOOLS_ORDER + 10;
    }

    @Override
    public String getShortcutKey() {
        return "a";
    }

    @Override
    public String getDescription() {
        return "Analyse with AI";
    }


}
