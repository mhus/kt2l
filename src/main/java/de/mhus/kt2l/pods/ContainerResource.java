package de.mhus.kt2l.pods;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;

public class ContainerResource implements KubernetesObject {

    private final PodGrid.Container container;

    public ContainerResource(PodGrid.Container container) {
        this.container = container;
    }

    @Override
    public V1ObjectMeta getMetadata() {
        return container.pod().getMetadata();
    }

    @Override
    public String getApiVersion() {
        return container.pod().getApiVersion();
    }

    @Override
    public String getKind() {
        return container.pod().getKind();
    }

    public String getContainerName() {
        return container.name();
    }


    public V1Pod getPod() {
        return container.pod();
    }
}
