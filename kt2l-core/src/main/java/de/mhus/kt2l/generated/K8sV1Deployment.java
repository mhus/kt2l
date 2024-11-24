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

import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;

@Slf4j
@Generated("de.mhus.kt2l.k8s.GenerateK8sHandlers")
public abstract class K8sV1Deployment implements HandlerK8s {

    @Autowired
    protected SecurityService securityService;

    @Override
    public V1APIResource getManagedType() {
        return K8s.DEPLOYMENT;
    }

    @Override
    public KubernetesObject get(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        return apiProvider.getAppsV1Api().readNamespacedDeployment(
            name
            ,namespace
        ).execute();
    }

    @Override
    public Object replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        var res = Yaml.loadAs(yaml, V1Deployment.class);
        return replaceResource(apiProvider, name, namespace, res);
    }

    public Object replaceResource(ApiProvider apiProvider, String name, String namespace, V1Deployment resource) throws ApiException {
        return apiProvider.getAppsV1Api().replaceNamespacedDeployment(
            name,
            namespace,
            resource
        ).execute();
    }

    @Override
    public Object delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        K8sUtil.checkDeleteAccess(securityService, K8s.DEPLOYMENT);
        return apiProvider.getAppsV1Api().deleteNamespacedDeployment(
            name
            ,namespace
        ).execute();
    }

    @Override
    public Object create(ApiProvider apiProvider, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1Deployment.class);
        return apiProvider.getAppsV1Api().createNamespacedDeployment(
            body.getMetadata().getNamespace(),
            body
        ).execute();
    }

    @Override
    public V1DeploymentList createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getAppsV1Api().listDeploymentForAllNamespaces().execute();
    }

    @Override
    public V1DeploymentList createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException {
        return apiProvider.getAppsV1Api().listNamespacedDeployment(
            namespace
        ).execute();
    }

    @Override
    public Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getAppsV1Api().listDeploymentForAllNamespaces().watch(true).buildCall(
            new CallBackAdapter<V1Deployment>(LOGGER)
        );
    }

    @Override
    public Object patch(ApiProvider apiProvider, String namespace, String name, String patchString) throws ApiException {
        var patch = new V1Patch(patchString);
        return PatchUtils.patch(
            V1Deployment.class,
            () -> apiProvider.getAppsV1Api().patchNamespacedDeployment(
                    name,
                    namespace,
                    patch
            ).buildCall(new CallBackAdapter<V1Deployment>(LOGGER)),
            V1Patch.PATCH_FORMAT_JSON_PATCH,
            apiProvider.getClient()
        );
    }

}
