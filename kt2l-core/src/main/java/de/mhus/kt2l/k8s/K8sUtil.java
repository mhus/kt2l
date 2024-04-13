package de.mhus.kt2l.k8s;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import de.mhus.commons.tools.MString;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1APIResourceList;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static de.mhus.commons.tools.MString.isEmpty;

@Slf4j
public class K8sUtil {

    public static final String NAMESPACE_ALL = "all";
    public static final String RESOURCE_PODS =  "pods";
    public static final String RESOURCE_NODES = "nodes";
    public static final String RESOURCE_NAMESPACE = "namespaces";
    public static final String RESOURCE_CONTAINER =  "container";
    public static final String WATCH_EVENT_ADDED = "ADDED";
    public static final String WATCH_EVENT_MODIFIED = "MODIFIED";
    public static final String WATCH_EVENT_DELETED = "DELETED";

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

    public static LinkedList<V1APIResource> getResourceTypes(CoreV1Api coreApi) {
        LinkedList<V1APIResource> types = new LinkedList<>();
        try {
            coreApi.getAPIResources().getResources().forEach(res -> types.add(res));
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
                    // add static resources
                    types.add(new V1APIResource().kind("Deployment").name("deployments").addShortNamesItem("deploy").version("v1").group("apps"));
                    types.add(new V1APIResource().kind("StatefulSet").name("statefulsets").version("v1").group("apps"));
                    types.add(new V1APIResource().kind("DaemonSet").name("daemonsets").version("v1").group("apps"));
                    types.add(new V1APIResource().kind("ReplicaSet").name("replicasets").version("v1").group("apps"));
                    types.add(new V1APIResource().kind("Job").name("jobs").version("v1").group("batch"));
                    types.add(new V1APIResource().kind("CronJob").name("cronjobs").version("v1").group("batch"));

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

    public static V1APIResource findResource(String resourceType, List<V1APIResource> resources) {
        return resources.stream().filter(r -> toResourceType(r).endsWith(resourceType)).findFirst().orElse(null);
    }

    public static String toYaml(KubernetesObject resource) {

        if (resource == null) return "";
        if (resource instanceof GenericObject) {
            Gson gson = new JSON().getGson();
            JsonElement jsonElement = gson.toJsonTree(((GenericObject)resource).getData());
            String jsonTxt = gson.toJson(jsonElement);
            return jsonTxt; //TODO
        }
        // get yaml

        var resContent = Yaml.dump(resource);
        return resContent;
    }

    public static String toResourceType(String group, String apiVersion, String plural) {
        if (isEmpty(group) && isEmpty(apiVersion))
            return plural;
        if (isEmpty(group))
            return apiVersion + "/" + plural;
        return group + "/" + apiVersion + "/" + plural;
    }

    public static String getAge(OffsetDateTime creationTimestamp) {
        final var age = System.currentTimeMillis()/1000 - creationTimestamp.toEpochSecond();
        if (age < 60) return age + "s";
        if (age < 3600) return age/60 + "m";
        if (age < 86400) return age/3600 + "h";
        return age/86400 + "d";
    }

}
