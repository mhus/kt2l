package de.mhus.kt2l.k8s;

import de.mhus.kt2l.config.AaaConfiguration;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.SecurityService;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.util.Yaml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KNode implements KHandler {

    @Autowired
    private SecurityService securityService;

    @Override
    public String getManagedKind() {
        return K8sUtil.KIND_NODE;
    }

    @Override
    public void replace(CoreV1Api api, String name, String namespace, String yaml) throws ApiException {
        // this is dangerous ... deny!
        if (!securityService.hasRole(AaaConfiguration.SCOPE_RESOURCE, K8sUtil.KIND_NODE, UsersConfiguration.ROLE.ADMIN.name()))
            throw new ApiException(403, "Access denied for non admin users");
        var body = Yaml.loadAs(yaml, V1Node.class);
        api.replaceNode(
                name,
                body, null, null, null, null
        );
    }

    @Override
    public void delete(CoreV1Api api, String name, String namespace) throws ApiException {
        // this is dangerous ... deny!
        if (!securityService.hasRole(AaaConfiguration.SCOPE_RESOURCE, K8sUtil.KIND_NODE, UsersConfiguration.ROLE.ADMIN.name()))
            throw new ApiException(403, "Access denied for non admin users");
        api.deleteNode(name, null, null, null, null, null, null);
    }
}
