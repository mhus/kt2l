package de.mhus.kt2l;

import com.vaadin.flow.component.icon.VaadinIcon;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class ActionLogs implements ResourceAction {
    @Override
    public boolean canHandleResourceType(String resourceType) {
        return K8sUtil.RESOURCE_PODS.equals(resourceType) || K8sUtil.RESOURCE_CONTAINER.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(String resourceType, Set<?> selected) {
        return canHandleResourceType(resourceType) && selected.size() > 0;
    }

    @Override
    public void execute(ExecutionContext context) {

        List<PodGrid.Container> containers = new ArrayList<>();

        if (context.getResourceType().equals(K8sUtil.RESOURCE_CONTAINER)) {
            context.getSelected().forEach(c -> containers.add((PodGrid.Container)c));
        } else
        if (context.getResourceType().equals(K8sUtil.RESOURCE_PODS)) {
            context.getSelected().forEach(p -> {
                       final var pod = (PodGrid.Pod)p;
                       pod.getPod().getStatus().getContainerStatuses().forEach(cs -> {
                           containers.add(new PodGrid.Container(
                                   cs.getName(),
                                   pod.getPod().getMetadata().getNamespace(),
                                   cs.getState().getWaiting() != null ? "Waiting" : "Running",
                                   null,
                                   0,
                                   pod.getPod()));
                       });
                    });
        }

        final var selected = (PodGrid.Pod)context.getSelected().iterator().next();
        context.getMainView().getTabBar().addTab(
                context.getClusterConfiguration().name() + ":" + selected.getName() + ":logs",
                selected.getName(),
                true,
                true,
                VaadinIcon.MODAL_LIST.create(),
                () ->
                new PodLogsPanel(
                        context.getClusterConfiguration(),
                        context.getApi(),
                        context.getMainView(),
                        containers
                        )).setColor(context.getClusterConfiguration().color()).select().setParentTab(context.getSelectedTab());
    }

    @Override
    public String getTitle() {
        return "Logs";
    }

    @Override
    public String getMenuBarPath() {
        return null;
    }

    @Override
    public String getShortcutKey() {
        return "l";
    }

    @Override
    public String getPopupPath() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Show container logs";
    }
}
