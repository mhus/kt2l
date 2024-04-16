package de.mhus.kt2l.k8s;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreApi;
import io.kubernetes.client.openapi.apis.CoreV1Api;

public interface KHandler {

    String getManagedKind();

    void replace(CoreV1Api api, String name, String namespace, String yaml) throws ApiException;

    void delete(CoreV1Api api, String name, String namespace) throws ApiException;

}
