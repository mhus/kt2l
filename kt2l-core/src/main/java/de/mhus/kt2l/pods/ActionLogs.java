package de.mhus.kt2l.pods;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.config.UsersConfiguration.ROLE;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.ui.PanelService;
import de.mhus.kt2l.ui.WithRole;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@WithRole(ROLE.READ)
public class ActionLogs implements ResourceAction {

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandleResourceType(String resourceType) {
        return K8sUtil.RESOURCE_PODS.equals(resourceType) || K8sUtil.RESOURCE_CONTAINER.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(String resourceType, Set<? extends KubernetesObject> selected) {
        return canHandleResourceType(resourceType) && selected.size() > 0;
    }

    @Override
    public void execute(ExecutionContext context) {

        List<ContainerResource> containers = new ArrayList<>();

        if (context.getResourceType().equals(K8sUtil.RESOURCE_CONTAINER)) {
            context.getSelected().forEach(c -> containers.add((ContainerResource)c));
        } else
        if (context.getResourceType().equals(K8sUtil.RESOURCE_PODS)) {
            context.getSelected().forEach(p -> {
               final var pod = (V1Pod)p;
               pod.getStatus().getContainerStatuses().forEach(cs -> {
                   containers.add(new ContainerResource(new PodGrid.Container(
                           PodGrid.CONTAINER_TYPE.DEFAULT,
                           cs,
                           pod)));
               });
                pod.getStatus().getEphemeralContainerStatuses().forEach(cs -> {
                    containers.add(new ContainerResource(new PodGrid.Container(
                            PodGrid.CONTAINER_TYPE.EPHEMERAL,
                            cs,
                            pod)));
                });
                pod.getStatus().getInitContainerStatuses().forEach(cs -> {
                    containers.add(new ContainerResource(new PodGrid.Container(
                            PodGrid.CONTAINER_TYPE.INIT,
                            cs,
                            pod)));
                });

            });
        }

        final var selected = (V1Pod)context.getSelected().iterator().next();
        panelService.addPanel(
                context.getSelectedTab(),
                context.getClusterConfiguration().name() + ":" + selected.getMetadata().getNamespace() + "." + selected.getMetadata().getName() + ":logs",
                selected.getMetadata().getName(),
                true,
                VaadinIcon.MODAL_LIST.create(),
                () ->
                new PodLogsPanel(
                        context.getClusterConfiguration(),
                        context.getApi(),
                        context.getMainView(),
                        containers
                        )).setHelpContext("logs").select();
    }

    @Override
    public String getTitle() {
        return "Logs;icon=" + VaadinIcon.MODAL_LIST;
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.VIEW_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.VIEW_ORDER+10;
    }

    @Override
    public String getShortcutKey() {
        return "l";
    }

    @Override
    public String getDescription() {
        return "Show container logs";
    }
}
