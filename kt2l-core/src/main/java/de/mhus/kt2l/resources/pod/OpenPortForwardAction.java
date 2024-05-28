package de.mhus.kt2l.resources.pod;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.portforward.PortForwardClusterAction;
import de.mhus.kt2l.portforward.PortForwardingPanel;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@WithRole(UsersConfiguration.ROLE.WRITE)
public class OpenPortForwardAction implements ResourceAction {

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandleResourceType(Cluster cluster, K8s resourceType) {
        return K8s.POD.equals(resourceType);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, K8s resourceType, Set<? extends KubernetesObject> selected) {
        if (!canHandleResourceType(cluster, resourceType))
            return false;
        if (selected.isEmpty()) return false;

        for (KubernetesObject obj : selected) {
            if (obj instanceof V1Pod pod) {
                if (pod.getSpec().getContainers() != null) {
                    for (var container : pod.getSpec().getContainers()) {
                        if (container.getPorts() != null && !container.getPorts().isEmpty()) {
                            return true;
                        }
                    }
                }
            } else
            if (obj instanceof ContainerResource container) {
                if (container.getContainer().getPorts() != null && !container.getContainer().getPorts().isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void execute(ExecutionContext context) {

        AtomicInteger nextPort = new AtomicInteger(9000);
        var selected = context.getSelected();
        StringBuilder cmd = new StringBuilder();


        for (KubernetesObject obj : selected) {
            if (obj instanceof V1Pod pod) {
                if (pod.getSpec().getContainers() != null) {
                    for (var container : pod.getSpec().getContainers()) {
                        if (container.getPorts() != null && !container.getPorts().isEmpty()) {
                            container.getPorts().forEach(port -> {
                                cmd
                                        .append("pod ")
                                        .append(pod.getMetadata().getNamespace()).append(" ")
                                        .append(pod.getMetadata().getName()).append(" ")
                                        .append(port.getContainerPort()).append(" ")
                                        .append(nextPort.getAndIncrement())
                                        .append("\n");
                            });
                        }
                    }
                }
            } else
            if (obj instanceof ContainerResource container) {
                if (container.getContainer().getPorts() != null && !container.getContainer().getPorts().isEmpty()) {
                    container.getContainer().getPorts().forEach(port -> {
                        cmd
                                .append("pod ")
                                .append(container.getPod().getMetadata().getNamespace()).append(" ")
                                .append(container.getPod().getMetadata().getName()).append(" ")
                                .append(port.getContainerPort()).append(" ")
                                .append(nextPort.getAndIncrement())
                                .append("\n");
                    });
                }
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
