package de.mhus.kt2l.resources.common;

import com.vaadin.flow.component.icon.AbstractIcon;
import de.mhus.commons.tools.MSystem;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.ui.BackgroundJobDialog;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
@WithRole(UsersConfiguration.ROLE.WRITE)
public class DummyAction implements ResourceAction  {
    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return MSystem.isVmDebug();
    }

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {
        return true;
    }

    @Override
    public void execute(ExecutionContext context) {
        BackgroundJobDialog dialog = new BackgroundJobDialog(context.getCore(), context.getCluster(), true);
        dialog.setHeaderTitle("Dummy");
        dialog.open();
       dialog.setMax(100);

        Thread.startVirtualThread(() -> {
            for (int i = 0; i < 100; i++) {
                if (dialog.isCanceled())
                    break;
                final int step = i;
                LOGGER.info("{} Step {}", MSystem.getObjectId(DummyAction.this), step);
                context.getUi().access(() -> dialog.next("Step " + step));
                MThread.sleep(1000);
            }
            context.getUi().access(() -> dialog.close());
       });

    }

    @Override
    public String getTitle() {
        return "Dummy";
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.ACTIONS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.ACTIONS_ORDER + 200;
    }

    @Override
    public String getShortcutKey() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public AbstractIcon getIcon() {
        return null;
    }
}
