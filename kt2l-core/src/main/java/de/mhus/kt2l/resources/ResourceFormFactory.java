package de.mhus.kt2l.resources;

import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.form.FormPanel;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;

public interface ResourceFormFactory {

    boolean canHandleType(Cluster cluster, V1APIResource type);

    boolean canHandleResource(Cluster cluster, V1APIResource type, KubernetesObject selected);

    FormPanel createForm(ExecutionContext context);

}
