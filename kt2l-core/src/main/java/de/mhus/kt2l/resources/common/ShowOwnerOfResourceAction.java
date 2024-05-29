package de.mhus.kt2l.resources.common;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.resources.ResourcesFilter;
import de.mhus.kt2l.resources.ResourcesGridPanel;
import io.kubernetes.client.common.KubernetesObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@WithRole(UsersConfiguration.ROLE.READ)
public class ShowOwnerOfResourceAction implements ResourceAction {

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandleResourceType(Cluster cluster, K8s resourceType) {
        return true;
    }

    @Override
    public boolean canHandleResource(Cluster cluster, K8s resourceType, Set<? extends KubernetesObject> selected) {
        if (!canHandleResourceType(cluster, resourceType) || selected.size() != 1) return false;
        var res = selected.iterator().next();
        var ownerReference = res.getMetadata().getOwnerReferences();
        if (ownerReference == null || ownerReference.isEmpty()) return false;
        var kind = ownerReference.get(0).getKind();
        return  kind.equals("ReplicaSet") ||
                kind.equals("Deployment") ||
                kind.equals("Job") ||
                kind.equals("DaemonSet") ||
                kind.equals("CronJob") ||
                kind.equals("StatefulSet");
    }

    @Override
    public void execute(ExecutionContext context) {
        if (!canHandleResource(context.getCluster(), context.getResourceType(), context.getSelected())) return;
        var res = context.getSelected().iterator().next();
        var ownerReference = res.getMetadata().getOwnerReferences();
        var kind = ownerReference.get(0).getKind();
        var uid = ownerReference.get(0).getUid();
        var name = res.getMetadata().getName();
        var namespace = res.getMetadata().getNamespace();

        switch (kind) {
            case "ReplicaSet":

                ((ResourcesGridPanel)context.getSelectedTab().getPanel()).showResources(K8s.REPLICA_SET, namespace, new ResourcesFilter() {
                    @Override
                    public boolean filter(KubernetesObject res) {
                        if (res instanceof io.kubernetes.client.openapi.models.V1ReplicaSet node) {
                            return node.getMetadata().getUid().equals(uid);
                        }
                        return false;
                    }

                    @Override
                    public String getDescription() {
                        return "Owner of " + name;
                    }
                });

                break;
            case "Deployment":

                ((ResourcesGridPanel)context.getSelectedTab().getPanel()).showResources(K8s.DEPLOYMENT, namespace, new ResourcesFilter() {
                    @Override
                    public boolean filter(KubernetesObject res) {
                        if (res instanceof io.kubernetes.client.openapi.models.V1Deployment node) {
                            return node.getMetadata().getUid().equals(uid);
                        }
                        return false;
                    }

                    @Override
                    public String getDescription() {
                        return "Owner of " + name;
                    }
                });
                break;

            case "StatefulSet":

                ((ResourcesGridPanel)context.getSelectedTab().getPanel()).showResources(K8s.STATEFUL_SET, namespace, new ResourcesFilter() {
                    @Override
                    public boolean filter(KubernetesObject res) {
                        if (res instanceof io.kubernetes.client.openapi.models.V1StatefulSet node) {
                            return node.getMetadata().getUid().equals(uid);
                        }
                        return false;
                    }

                    @Override
                    public String getDescription() {
                        return "Owner of " + name;
                    }
                });
                break;

            case "Job":

                ((ResourcesGridPanel)context.getSelectedTab().getPanel()).showResources(K8s.JOB, namespace, new ResourcesFilter() {
                    @Override
                    public boolean filter(KubernetesObject res) {
                        if (res instanceof io.kubernetes.client.openapi.models.V1Job node) {
                            return node.getMetadata().getUid().equals(uid);
                        }
                        return false;
                    }

                    @Override
                    public String getDescription() {
                        return "Owner of " + name;
                    }
                });
                break;

            case "CronJob":

                ((ResourcesGridPanel)context.getSelectedTab().getPanel()).showResources(K8s.CRON_JOB, namespace, new ResourcesFilter() {
                    @Override
                    public boolean filter(KubernetesObject res) {
                        if (res instanceof io.kubernetes.client.openapi.models.V1CronJob node) {
                            return node.getMetadata().getUid().equals(uid);
                        }
                        return false;
                    }

                    @Override
                    public String getDescription() {
                        return "Owner of " + name;
                    }
                });
                break;

            case "DaemonSet":

                ((ResourcesGridPanel)context.getSelectedTab().getPanel()).showResources(K8s.DAEMON_SET, namespace, new ResourcesFilter() {
                    @Override
                    public boolean filter(KubernetesObject res) {
                        if (res instanceof io.kubernetes.client.openapi.models.V1DaemonSet node) {
                            return node.getMetadata().getUid().equals(uid);
                        }
                        return false;
                    }

                    @Override
                    public String getDescription() {
                        return "Owner of " + name;
                    }
                });
                break;

        }
    }

    @Override
    public String getTitle() {
        return "Show Owner;icon=" + VaadinIcon.ARROW_BACKWARD.name();
    }

    @Override
    public String getMenuPath() {
        return VIEW_PATH;
    }

    @Override
    public int getMenuOrder() {
        return 100;
    }

    @Override
    public String getShortcutKey() {
        return "CTRL+O";
    }

    @Override
    public String getDescription() {
        return "Show owner of the resource";
    }
}
