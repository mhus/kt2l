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

import io.kubernetes.client.openapi.models.V1StorageClass;
import io.kubernetes.client.openapi.models.V1StorageClassList;

@Slf4j
public abstract class K8sV1StorageClass implements HandlerK8s {

    @Autowired
    private SecurityService securityService;

    @Override
    public K8s getManagedType() {
        return K8s.STORAGE_CLASS;
    }

    @Override
    public Object replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1StorageClass.class);
        return apiProvider.getStorageV1Api().replaceStorageClass(
            name,
            body,
            null, null, null, null
        );
    }

    @Override
    public Object delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        K8sUtil.checkDeleteAccess(securityService, K8s.STORAGE_CLASS);
        return apiProvider.getStorageV1Api().deleteStorageClass(
            name,
            null, null, null, null, null, null
        );
    }

    @Override
    public Object create(ApiProvider apiProvider, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1StorageClass.class);
        return apiProvider.getStorageV1Api().createStorageClass(
            body,
            null, null, null, null
        );
    }

    @Override
    public V1StorageClassList createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getStorageV1Api().listStorageClass(
            null, null, null, null, null, null, null, null, null, null, null
        );
    }

    @Override
    public V1StorageClassList createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException {
      throw new NotImplementedException();
    }

    @Override
    public Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getStorageV1Api().listStorageClassCall(
            null, null, null, null, null, null, null, null, null, null, true,
            new CallBackAdapter<V1StorageClass>(LOGGER)
        );
    }

    @Override
    public Object patch(ApiProvider apiProvider, String namespace, String name, String patchString) throws ApiException {
        var patch = new V1Patch(patchString);
        return apiProvider.getStorageV1Api().patchStorageClassCall(
            name,
            patch,
            null, null, null, null, null, null
        );
    }

}
