package de.mhus.kt2l;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;

import java.util.ArrayList;
import java.util.List;

public class K8sUtil {
    public static List<String> geNamespacesWithAll(CoreV1Api coreApi) {
        List<String> namespaces = new ArrayList<>(10);
        namespaces.add("all");
        try {
            coreApi.listNamespace(null, null, null, null, null, null, null, null, null, null)
                    .getItems().forEach(ns -> namespaces.add(ns.getMetadata().getName()));
        } catch (ApiException e) {
        }
        return namespaces;
    }
}
