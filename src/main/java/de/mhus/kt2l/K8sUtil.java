package de.mhus.kt2l;

import de.mhus.commons.tools.MString;
import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1APIResourceList;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class K8sUtil {

    public static final String NAMESPACE_ALL = "all";
    public static final String RESOURCE_PODS =  "pods";

    public static LinkedList<String> getNamespaces(CoreV1Api coreApi) {
        LinkedList<String> namespaces = new LinkedList<>();
        try {
            coreApi.listNamespace(null, null, null, null, null, null, null, null, null, null)
                    .getItems().forEach(ns -> namespaces.add(ns.getMetadata().getName()));
        } catch (ApiException e) {
        }
        return namespaces;
    }

    public static CompletableFuture<LinkedList<String>> getNamespacesAsync(CoreV1Api coreApi) {
        CompletableFuture<LinkedList<String>> future = new CompletableFuture<>();
        try {
            coreApi.listNamespaceAsync(null, null, null, null, null, null, null, null, null, null, new ApiCallback<>() {
                @Override
                public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
                }
                @Override
                public void onSuccess(V1NamespaceList result, int statusCode, Map<String, List<String>> responseHeaders) {
                    LinkedList<String> types = new LinkedList<>();
                    types.addAll(result.getItems().stream().map(r -> r.getMetadata().getName()).collect(Collectors.toList()));
                    future.complete(types);
                }
                @Override
                public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
                }
                @Override
                public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
                }
            });
        } catch (ApiException e) {
            LOGGER.warn("Error getting namespaces", e);
        }
        return future;
    }

    public static LinkedList<String> getResourceTypes(CoreV1Api coreApi) {
        LinkedList<String> types = new LinkedList<>();
        try {
            coreApi.getAPIResources().getResources().forEach(res -> types.add(res.getKind()));
        } catch (ApiException e) {
            LOGGER.error("Error getting resource types", e);
        }
        return types;
    }

    public static CompletableFuture<LinkedList<V1APIResource>> getResourceTypesAsync(CoreV1Api coreApi) {

        CompletableFuture<LinkedList<V1APIResource>> future = new CompletableFuture<>();
        try {
            coreApi.getAPIResourcesAsync(new ApiCallback<V1APIResourceList>() {
                @Override
                public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) { }

                @Override
                public void onSuccess(V1APIResourceList result, int statusCode, Map<String, List<String>> responseHeaders) {
                    LinkedList<V1APIResource> types = new LinkedList<>();
                    types.addAll(result.getResources().stream().collect(Collectors.toList()));
                    future.complete(types);
                }

                @Override
                public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {}
                @Override
                public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {}
            });
        } catch (ApiException e) {
            LOGGER.error("Error getting resource types", e);
        }
        return future;
    }

    public static String toResourceType(V1APIResource resource) {
        if (resource == null)
            return null;
        if (MString.isSet(resource.getGroup()))
            return resource.getGroup() + "/" + resource.getVersion() + "/" + resource.getName();
        if (resource.getVersion() != null && !resource.getVersion().equals("v1"))
            return resource.getVersion() + "/" + resource.getName();
        return resource.getName();
    }

    public static V1APIResource findResource(String resourceType, LinkedList<V1APIResource> resources) {
        return resources.stream().filter(r -> toResourceType(r).equals(resourceType)).findFirst().orElse(null);
    }

}
