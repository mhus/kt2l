package de.mhus.kt2l;

import com.vaadin.flow.component.Component;
import io.kubernetes.client.openapi.apis.CoreV1Api;

public interface ResourcesGrid {

    Component getComponent();

    void refresh();

    void init(CoreV1Api coreApi, ClusterConfiguration.Cluster clusterConfig, ResourcesView view);

    void setFilter(String value);

    void setNamespace(String value);

    void setResourceType(String resourceType);
}
