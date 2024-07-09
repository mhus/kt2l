package de.mhus.kt2l.resources;

import de.mhus.kt2l.cluster.Cluster;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;

public interface ResourceForm {

    boolean canHandleType(Cluster cluster, V1APIResource type);

    boolean canHandleResource(Cluster cluster, V1APIResource type, KubernetesObject selected);

    void execute(ExecutionContext context);

}
