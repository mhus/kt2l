package de.mhus.kt2l.generated;

import de.mhus.kt2l.aaa.SecurityService;
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
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.NotImplementedException;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.annotation.Generated;

import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1DaemonSetList;

@Slf4j
@Generated("de.mhus.kt2l.k8s.GenerateK8sHandlers")
public abstract class K8sV1DaemonSet implements HandlerK8s {

    @Autowired
    protected SecurityService securityService;

    @Override
    public V1APIResource getManagedType() {
        return K8s.DAEMON_SET;
    }

    @Override
    public KubernetesObject get(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        return apiProvider.getAppsV1Api().readNamespacedDaemonSet(
            name
            ,namespace
        ).execute();
    }

    @Override
    public Object replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        var res = Yaml.loadAs(yaml, V1DaemonSet.class);
        return replaceResource(apiProvider, name, namespace, res);
    }

    public Object replaceResource(ApiProvider apiProvider, String name, String namespace, V1DaemonSet resource) throws ApiException {
        return apiProvider.getAppsV1Api().replaceNamespacedDaemonSet(
            name,
            namespace,
            resource
        ).execute();
    }

    @Override
    public Object delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        K8sUtil.checkDeleteAccess(securityService, K8s.DAEMON_SET);
        return apiProvider.getAppsV1Api().deleteNamespacedDaemonSet(
            name
            ,namespace
        ).execute();
    }

    @Override
    public Object create(ApiProvider apiProvider, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1DaemonSet.class);
        return apiProvider.getAppsV1Api().createNamespacedDaemonSet(
            body.getMetadata().getNamespace(),
            body
        ).execute();
    }

    @Override
    public V1DaemonSetList createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getAppsV1Api().listDaemonSetForAllNamespaces().execute();
    }

    @Override
    public V1DaemonSetList createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException {
        return apiProvider.getAppsV1Api().listNamespacedDaemonSet(
            namespace
        ).execute();
    }

    @Override
    public Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getAppsV1Api().listDaemonSetForAllNamespaces().watch(true).buildCall(
            new CallBackAdapter<V1DaemonSet>(LOGGER)
        );
    }

    @Override
    public Object patch(ApiProvider apiProvider, String namespace, String name, String patchString) throws ApiException {
        var patch = new V1Patch(patchString);
        return PatchUtils.patch(
            V1DaemonSet.class,
            () -> apiProvider.getAppsV1Api().patchNamespacedDaemonSet(
                    name,
                    namespace,
                    patch
            ).buildCall(new CallBackAdapter<V1DaemonSet>(LOGGER)),
            V1Patch.PATCH_FORMAT_JSON_PATCH,
            apiProvider.getClient()
        );
    }

}
