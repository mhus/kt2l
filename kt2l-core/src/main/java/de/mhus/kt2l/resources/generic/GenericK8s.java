package de.mhus.kt2l.resources.generic;

import de.mhus.commons.yaml.MYaml;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1Status;

import java.util.Arrays;

public class GenericK8s implements HandlerK8s {

    private final V1APIResource resourceType;

    public GenericK8s(V1APIResource resourceType) {
        this.resourceType = resourceType;
    }
    @Override
    public K8s.RESOURCE getManagedKind() {
        return K8s.RESOURCE.GENERIC;
    }

    @Override
    public void replace(CoreV1Api api, String name, String namespace, String yaml) throws ApiException {
        var y = MYaml.loadFromString(yaml);
//        var kind = y.asMap().getString("Kind");
//        var resource = Arrays.stream(K8s.RESOURCE.values()).filter(r -> r.kind().equalsIgnoreCase(kind)).findFirst().orElse(null);
//        if (resource == null) throw new ApiException("Kind not found: " + kind);
//        var v1Resource = K8s.toResource(resource);
        var genericApi = new GenericObjectsApi(api.getApiClient(), resourceType );
        genericApi.replace(name, namespace, yaml);
    }

    @Override
    public V1Status delete(CoreV1Api api, String name, String namespace) throws ApiException {
        var genericApi = new GenericObjectsApi(api.getApiClient(), resourceType );
        genericApi.delete(name, namespace);
        return new V1Status(); //XXX
    }
}
