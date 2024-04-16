package de.mhus.kt2l.k8s;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreApi;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Yaml;
import org.springframework.stereotype.Component;

@Component
public class KPod implements KHandler {

    @Override
    public String getManagedKind() {
        return K8sUtil.KIND_POD;
    }

    @Override
    public void replace(CoreV1Api api, String name, String namespace, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1Pod.class);
        api.replaceNamespacedPod(
                name,
                namespace,
                body, null, null, null, null
        );
    }

    @Override
    public void delete(CoreV1Api api, String name, String namespace) throws ApiException {
        api.deleteNamespacedPod(name, namespace, null, null, null, null, null, null);
    }

}
