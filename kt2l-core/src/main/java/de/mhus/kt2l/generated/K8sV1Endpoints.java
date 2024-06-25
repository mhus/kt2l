package de.mhus.kt2l.generated;

import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.CallBackAdapter;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.PatchUtils;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.NotImplementedException;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import io.kubernetes.client.openapi.models.V1Endpoints;
import io.kubernetes.client.openapi.models.V1EndpointsList;

@Slf4j
public abstract class K8sV1Endpoints implements HandlerK8s {

    @Autowired
    private SecurityService securityService;

    @Override
    public K8s getManagedType() {
        return K8s.ENDPOINTS;
    }

    @Override
    public Object replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1Endpoints.class);
        return apiProvider.getCoreV1Api().replaceNamespacedEndpoints(
            name,
            namespace,
            body,
            null, null, null, null
        );
    }

    @Override
    public Object delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        K8sUtil.checkDeleteAccess(securityService, K8s.ENDPOINTS);
        return apiProvider.getCoreV1Api().deleteNamespacedEndpoints(
            name,
            namespace,
            null, null, null, null, null, null
        );
    }

    @Override
    public Object create(ApiProvider apiProvider, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1Endpoints.class);
        return apiProvider.getCoreV1Api().createNamespacedEndpoints(
            body.getMetadata().getNamespace(),
            body,
            null, null, null, null
        );
    }

    @Override
    public V1EndpointsList createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getCoreV1Api().listEndpointsForAllNamespaces(
            null, null, null, null, null, null, null, null, null, null, null
        );
    }

    @Override
    public V1EndpointsList createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException {
        return apiProvider.getCoreV1Api().listNamespacedEndpoints(
            namespace,
            null, null, null, null, null, null, null, null, null, null, null
        );
    }

    @Override
    public Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getCoreV1Api().listEndpointsForAllNamespacesCall(
            null, null, null, null, null, null, null, null, null, null, true,
            new CallBackAdapter<V1Endpoints>(LOGGER)
        );
    }

    @Override
    public Object patch(ApiProvider apiProvider, String namespace, String name, String patchString) throws ApiException {
        var patch = new V1Patch(patchString);
        return apiProvider.getCoreV1Api().patchNamespacedEndpointsCall(
            name,
            namespace,
            patch,
            null, null, null, null, null, null
        );
    }

}
