package de.mhus.kt2l.resources;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ShortcutEvent;
import de.mhus.kt2l.cluster.ClusterConfiguration;
import io.kubernetes.client.openapi.apis.CoreV1Api;

public interface ResourcesGrid {

    Component getComponent();

    void refresh(long counter);

    void init(CoreV1Api coreApi, ClusterConfiguration.Cluster clusterConfig, ResourcesGridPanel view);

    void setFilter(String value, ResourcesFilter resourcesFilter);

    void setNamespace(String value);

    void setResourceType(String resourceType);

    void handleShortcut(ShortcutEvent event);

    void setSelected();

    void setUnselected();

    void destroy();
}
