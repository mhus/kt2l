package de.mhus.kt2l.pods;

import com.vaadin.flow.component.notification.Notification;
import de.mhus.commons.tools.MCollection;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tools.MSystem;
import de.mhus.commons.tree.MProperties;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.config.ConfigUtil;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.generic.ExecutionContext;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.generic.ResourceAction;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Slf4j
@Component
public class ActionTerminal implements ResourceAction {

    @Autowired
    private Configuration configuration;


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

        V1Pod pod = null;
        String container = null;
        String containerImage = null;
        if (K8sUtil.RESOURCE_PODS.equals(context.getResourceType())) {
            pod = (V1Pod) context.getSelected().iterator().next();
            container = pod.getStatus().getContainerStatuses().get(0).getName();
            containerImage = pod.getStatus().getContainerStatuses().get(0).getImage();
        } else {
            var containerResource = (ContainerResource)context.getSelected().iterator().next();
            pod = containerResource.getPod();
            container = containerResource.getContainerName();
            final String finalContainer = container;
            containerImage = containerResource.getPod().getStatus().getContainerStatuses().stream().filter(c -> c.getName().equals(finalContainer)).findFirst().get().getImage();
        }
        final var conf = configuration.getSection("cmd-" + MSystem.getOS().name());
        final var shell = ConfigUtil.getShellFor(configuration, context.getClusterConfiguration(), pod, containerImage);
        final var vars = new MProperties();
        vars.setString("pod", pod.getMetadata().getName());
        vars.setString("container", container);
        vars.setString("namespace", pod.getMetadata().getNamespace());
        vars.setString("context", context.getClusterConfiguration().name());
        vars.setString("cmd", shell);
        final var osCmd = MTree.getArrayValueStringList(conf.getArray("exec").orElse(MTree.EMPTY_LIST));
        final String[] osCmdArray = osCmd.toArray(new String[0]);
        MCollection.replaceAll(osCmdArray, v -> MString.substitute(v, vars, v) );
        LOGGER.info("Execute: {}", Arrays.toString(osCmdArray));

        try {
            var res = MSystem.execute(osCmdArray);
            LOGGER.info("Result: {}", res);
            if (res.getRc() != 0)
                Notification.show("Failed to start Terminal");
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage());
            LOGGER.error("Error execute {}", Arrays.toString(osCmdArray), e);
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
        return "Terminal";
    }

    @Override
    public String getMenuBarPath() {
        return null;
    }

    @Override
    public String getShortcutKey() {
        return "t";
    }

    @Override
    public String getPopupPath() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Open shell in terminal";
    }
}
