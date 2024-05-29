package de.mhus.kt2l.resources.service;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.config.ViewsConfiguration;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.portforward.PortForwardBackgroundJob;
import de.mhus.kt2l.portforward.PortForwardClusterAction;
import de.mhus.kt2l.portforward.PortForwardingPanel;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.resources.pod.ContainerResource;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@WithRole(UsersConfiguration.ROLE.WRITE)
public class OpenSvcPortForwardAction implements ResourceAction {

    @Autowired
    private PanelService panelService;

    @Autowired
    private ViewsConfiguration viewsConfiguration;

    @Override
    public boolean canHandleResourceType(Cluster cluster, K8s resourceType) {
        return K8s.SERVICE.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, K8s resourceType, Set<? extends KubernetesObject> selected) {
        if (!canHandleResourceType(cluster, resourceType))
            return false;
        if (selected.isEmpty()) return false;

        for (KubernetesObject obj : selected) {
            if (obj instanceof V1Service service) {
                if (service.getSpec().getSelector() != null)
                    return true;
            }
        }

        return false;
    }

    @Override
    public void execute(ExecutionContext context) {

        // var portForwarder = context.getCore().backgroundJobInstance(context.getCluster(), PortForwardBackgroundJob.class);

        AtomicInteger nextPort = new AtomicInteger(viewsConfiguration.getConfig("portForward").getInt("firstPort", 9000));
        var selected = context.getSelected();
        StringBuilder cmd = new StringBuilder();

        for (KubernetesObject obj : selected) {

            if (obj instanceof V1Service service) {
                service.getSpec().getPorts().forEach(port -> {
                    cmd
                            .append("svc ")
                            .append(service.getMetadata().getNamespace()).append(" ")
                            .append(service.getMetadata().getName()).append(" ")
                            .append(port.getPort()).append(" ")
                            .append(nextPort.getAndIncrement())
                            .append("\n");
                });
            }
        }

        var tab = PortForwardClusterAction.openPanel(panelService, context.getCore(), context.getCluster()).select();

        ((PortForwardingPanel)tab.getPanel()).setCommand(cmd.toString());

    }

    @Override
    public String getTitle() {
        return "Port Forward;icon=" + VaadinIcon.CLOUD_UPLOAD_O.name();
    }

    @Override
    public String getMenuPath() {
        return ACTIONS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return 3263;
    }

    @Override
    public String getShortcutKey() {
        return "P";
    }

    @Override
    public String getDescription() {
        return "Open port forwarder panel and prepare the command";
    }
}
