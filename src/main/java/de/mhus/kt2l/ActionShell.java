package de.mhus.kt2l;

import com.vaadin.flow.component.icon.VaadinIcon;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class ActionShell implements ResourceAction {

    @Autowired
    private Configuration configuration;


    @Override
    public boolean canHandleResourceType(String resourceType) {
        return
                K8sUtil.RESOURCE_PODS.equals(resourceType) || K8sUtil.RESOURCE_CONTAINER.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(String resourceType, Set<?> selected) {
        return canHandleResourceType(resourceType) && selected.size() == 1;
    }

    @Override
    public void execute(ExecutionContext context) {

        var selected = (PodGrid.Pod)context.getSelected().iterator().next();

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
                        )).setColor(context.getClusterConfiguration().color()).select().setParentTab(context.getSelectedTab());
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
