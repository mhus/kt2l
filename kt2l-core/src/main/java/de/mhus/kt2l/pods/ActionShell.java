package de.mhus.kt2l.pods;

import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.generic.ExecutionContext;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.generic.ResourceAction;
import de.mhus.kt2l.ui.PanelService;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class ActionShell implements ResourceAction {

    @Autowired
    private Configuration configuration;

    @Autowired
    private PanelService panelService;


    @Override
    public boolean canHandleResourceType(String resourceType) {
        return
                K8sUtil.RESOURCE_PODS.equals(resourceType) || K8sUtil.RESOURCE_CONTAINER.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(String resourceType, Set<? extends KubernetesObject> selected) {
        return canHandleResourceType(resourceType) && selected.size() == 1;
    }

    @Override
    public void execute(ExecutionContext context) {

        var selected = (V1Pod)context.getSelected().iterator().next();

        panelService.addPanel(
                context.getSelectedTab(),
                context.getClusterConfiguration().name() + ":" + selected.getMetadata().getNamespace() + "." + selected.getMetadata().getName() + ":shell",
                selected.getMetadata().getName(),
                true,
                VaadinIcon.TERMINAL.create(),
                () -> new ContainerShellPanel(
                        context.getClusterConfiguration(),
                        context.getApi(),
                        context.getMainView(),
                        selected
                        )).select();
    }

    @Override
    public String getTitle() {
        return "Shell";
    }

    @Override
    public String getMenuBarPath() {
        return null;
    }

    @Override
    public String getShortcutKey() {
        return "s";
    }

    @Override
    public String getPopupPath() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Open shell";
    }
}
