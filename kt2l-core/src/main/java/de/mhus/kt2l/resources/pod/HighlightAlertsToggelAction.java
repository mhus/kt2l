package de.mhus.kt2l.resources.pod;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.UiUtil;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.resources.pod.score.PodScorerConfiguration;
import io.kubernetes.client.common.KubernetesObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@WithRole(UsersConfiguration.ROLE.READ)
public class HighlightAlertsToggelAction implements ResourceAction {

    @Autowired
    private PodScorerConfiguration podScorerConfiguration;

    @Override
    public boolean canHandleResourceType(Cluster cluster, K8s resourceType) {
        return K8s.POD.equals(resourceType) && podScorerConfiguration.isEnabled();
    }

    @Override
    public boolean canHandleResource(Cluster cluster, K8s resourceType, Set<? extends KubernetesObject> selected) {
        return canHandleResourceType(cluster, resourceType);
    }

    @Override
    public void execute(ExecutionContext context) {
        if (context.getGrid() instanceof PodGrid podGrid) {
            podGrid.setHighlightAlerts(!podGrid.isHighlightAlerts());
            podGrid.doRefreshGrid();
            UiUtil.showSuccessNotification("Highlighting alerts " + (podGrid.isHighlightAlerts() ? "enabled" : "disabled"));
        }
    }

    @Override
    public String getTitle() {
        return "Highlight Alerts;icon=" + VaadinIcon.ALARM;
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.VIEW_PATH;
    }

    @Override
    public int getMenuOrder() {
        return 50000;
    }

    @Override
    public String getShortcutKey() {
        return "CTRL+A";
    }

    @Override
    public String getDescription() {
        return "Toggle Highlight Alerts";
    }
}
