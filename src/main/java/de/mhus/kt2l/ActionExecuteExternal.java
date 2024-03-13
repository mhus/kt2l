package de.mhus.kt2l;

import com.vaadin.flow.component.notification.Notification;
import de.mhus.commons.tools.MSystem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class ActionExecuteExternal implements XUiAction {
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

        if (MSystem.isMac()) {
            String[] command = new String[]{
                    "/usr/bin/osascript",
                    "-e",
                    "tell app \"Terminal\" to do script \"kubectl exec -it " + " --context '" + context.getClusterConfiguration().name() + "' " + selected.getName() + " -- /bin/sh && exit\"",
                    "-e",
                    "tell app \"Terminal\" to activate"
            };
            try {
                var res = MSystem.execute(command);
                LOGGER.info("Result: {}", res);
                if (res.getRc() != 0)
                    Notification.show("Failed to start Terminal");
            } catch (Exception e) {
                Notification.show("Error: " + e.getMessage());
                LOGGER.error("Error execute {}", command, e);
            }
        } else {
            Notification.show("Not supported on this platform");
        }

//        context.getMainView().getTabBar().addTab(
//                context.getClusterConfiguration().name() + ":" + selected.getName() + ":logs",
//                selected.getName(),
//                true,
//                true,
//                VaadinIcon.NOTEBOOK.create(),
//                () ->
//                new ContainerExecuteView(
//                        context.getClusterConfiguration(),
//                        context.getApi(),
//                        context.getMainView(),
//                        selected
//                        )).select().setParentTab(context.getSelectedTab());
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
