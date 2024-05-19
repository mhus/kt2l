package de.mhus.kt2l.core;

import com.vaadin.flow.component.icon.Icon;
import de.mhus.kt2l.cluster.ClusterOverviewPanel;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import io.kubernetes.client.common.KubernetesObject;

import java.util.Set;

public interface CoreAction {

    boolean canHandle(Core core);

    String getTitle();

    void execute(Core core);

    Icon getIcon();

    int getPriority();

}
