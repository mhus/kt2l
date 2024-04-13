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
        return container.getPod().getMetadata();
    }

    @Override
    public String getApiVersion() {
        return container.getPod().getApiVersion();
    }

    @Override
    public String getKind() {
        return container.getPod().getKind();
    }

    public String getContainerName() {
        return container.getName();
    }


    public V1Pod getPod() {
        return container.getPod();
    }
}
