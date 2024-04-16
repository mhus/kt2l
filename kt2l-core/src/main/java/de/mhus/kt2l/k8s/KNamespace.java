package de.mhus.kt2l.k8s;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.util.Yaml;
import org.springframework.stereotype.Component;

@Component
public class KNamespace implements KHandler {
    @Override
    public String getManagedKind() {
        return K8sUtil.KIND_NAMESPACE;
    }

    @Override
    public void replace(CoreV1Api api, String name, String namespace, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1Namespace.class);
        api.replaceNamespace(
                name,
                body, null, null, null, null
        );
    }

    @Override
    public void delete(CoreV1Api api, String name, String namespace) throws ApiException {
        api.deleteNamespace(name, null, null, null, null, null, null);
    }
}
