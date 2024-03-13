package de.mhus.kt2l;

import com.vaadin.flow.component.icon.VaadinIcon;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ActionExecute implements XUiAction {
    @Override
    public boolean canHandleResourceType(String resourceType) {
        return K8sUtil.RESOURCE_PODS.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(String resourceType, Set<?> selected) {
        return canHandleResourceType(resourceType) && selected.size() == 1;
    }

    @Override
    public void execute(ExecutionContext context) {

        final var selected = (PodGrid.Pod)context.getSelected().iterator().next();
        context.getMainView().getTabBar().addTab(
                context.getClusterConfiguration().name() + ":" + selected.getName() + ":logs",
                selected.getName(),
                true,
                true,
                VaadinIcon.NOTEBOOK.create(),
                () ->
                new ContainerExecuteView(
                        context.getClusterConfiguration(),
                        context.getApi(),
                        context.getMainView(),
                        selected
                        )).select().setParentTab(context.getSelectedTab());
    }

    @Override
    public String getTitle() {
        return "Exec";
    }

    @Override
    public String getMenuBarPath() {
        return "";
    }

    @Override
    public String getShortcutKey() {
        return "";
    }

    @Override
    public String getPopupPath() {
        return "";
    }
}
