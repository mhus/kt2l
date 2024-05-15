/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.mhus.kt2l.k8s;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import de.mhus.commons.console.ConsoleTable;
import de.mhus.commons.errors.NotFoundRuntimeException;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.resources.generic.GenericObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1APIResourceList;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static de.mhus.commons.tools.MString.isEmpty;

@Slf4j
public class K8sUtil {

    public static final String WATCH_EVENT_ADDED = "ADDED";
    public static final String WATCH_EVENT_MODIFIED = "MODIFIED";
    public static final String WATCH_EVENT_DELETED = "DELETED";

    public static K8s toResourceType(String resourceType) {
        if (isEmpty(resourceType))
            throw new NullPointerException("Resource type is empty");
        return Arrays.stream(K8s.values()).filter(r -> r.resourceType().equals(resourceType) || r.kind().equals(resourceType)).findFirst()
                .orElseThrow(() -> new NotFoundRuntimeException("Unknown resource type: " + resourceType));
    }

    public static V1APIResource toResource(KubernetesObject o, Cluster cluster) {
        if (cluster.getResourceTypes() == null)
            throw new IllegalArgumentException("ResourceTypes not found in cluster configuration");
        var kind = o.getKind();
        if (kind != null) {
            var resourceType = cluster.getResourceTypes().stream().filter(r -> r.getKind().equalsIgnoreCase(kind)).findFirst().orElse(null);
            if (resourceType != null) return resourceType;
        }
        var resource = Arrays.stream(K8s.values()).filter(r -> r.clazz().equals(o.getClass())).findFirst().orElse(null);
        if (resource != null) return K8s.toResource(resource);

        throw new IllegalArgumentException("Kind not found in cluster: " + kind);
    }

    public static void previewHeader(ApiProvider apiProvider, HandlerK8s handler, KubernetesObject res, StringBuilder sb) {
        var kind = res.getKind();
        if (kind != null) {
            sb.append("Kind:          ").append(kind).append("\n");
        }
        var name = res.getMetadata().getName();
            sb.append("Name:          ").append(name).append("\n");
        var namespace = res.getMetadata().getNamespace();
        if (namespace != null) {
            sb.append("Namespace:     ").append(namespace).append("\n");
        }
        var creationTimestamp = res.getMetadata().getCreationTimestamp();
        if (creationTimestamp != null) {
            sb.append("Created:       ").append(creationTimestamp).append("\n");
        }

        var labels = res.getMetadata().getLabels();
        if (labels != null) {
            sb.append("Labels:        ");
            boolean first = true;
            for (var e : labels.entrySet()) {
                if (!first)
                    sb.append("               ");
                first = false;
                sb.append(e.getKey()).append("=").append(e.getValue()).append("\n");
            }
        } else
            sb.append("Labels:        <none>\n");
        var annotations = res.getMetadata().getAnnotations();
        if (annotations != null) {
            sb.append("Annotations:   ");
            boolean first = true;
            for (var e : annotations.entrySet()) {
                if (!first)
                    sb.append("               ");
                first = false;
                sb.append(e.getKey()).append("=").append(e.getValue()).append("\n");
            }
        } else
            sb.append("Annotations:   <none>\n");
    }

    public static void previewFooter(ApiProvider apiProvider, HandlerK8s handler, KubernetesObject res, StringBuilder sb) {

        try {
            final var uid = res.getMetadata().getUid();
            final var namespace = res.getMetadata().getNamespace();
            final var fieldSelector = "involvedObject.uid=" + uid;
            final var list = namespace == null ?
                    apiProvider.getCoreV1Api().listEventForAllNamespaces( null, null, null, fieldSelector, 10, null, null, null, null, 10, null)
                    :
                    apiProvider.getCoreV1Api().listNamespacedEvent( namespace, null, null, null, fieldSelector, null, 10, null, null, null, 10, null);

            final var events = list.getItems();
            if (events != null && events.size() > 0) {
                sb.append("\nEvents:\n\n");
                ConsoleTable table = new ConsoleTable();
                table.setHeaderValues("Type", "Reason", "Age", "Count", "From", "Message");
                events.forEach(event -> {
                    table.addRowValues(
                            event.getType(),
                            event.getReason(),
                            getAge(event.getMetadata().getCreationTimestamp()),
                            event.getCount(),
                            event.getSource().getComponent(),
                            event.getMessage()
                    );
                });
                sb.append(table.toString()).append("\n");
            }
        } catch (ApiException e) {
            LOGGER.error("Error getting events for {}", res, e);
        }
    }

    public static final String NAMESPACE_ALL = "[all]";


    /**
     * not public to force security checks, use K8sService instead.
     */
    static List<String> getNamespaces(CoreV1Api coreApi) {
        LinkedList<String> namespaces = new LinkedList<>();
        try {
            coreApi.listNamespace(null, null, null, null, null, null, null, null, null, null, null)
                    .getItems().forEach(ns -> namespaces.add(ns.getMetadata().getName()));
        } catch (ApiException e) {
            LOGGER.warn("Error getting namespaces", e);
        }
        return namespaces;
    }

    /**
     * not public to force security checks, use K8sService instead.
     */
    static CompletableFuture<List<String>> getNamespacesAsync(CoreV1Api coreApi) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        try {
            coreApi.listNamespaceAsync(null, null, null, null, null, null, null, null, null, null, null, new ApiCallback<V1NamespaceList>() {
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

    /**
     * not public to force security checks, use K8sService instead.
     */
    static List<V1APIResource> getResourceTypes(CoreV1Api coreApi) {
        LinkedList<V1APIResource> types = new LinkedList<>();
        try {
            coreApi.getAPIResources().getResources().forEach(res -> types.add(res));
        } catch (ApiException e) {
            LOGGER.error("Error getting resource types", e);
        }
        return types;
    }

    /**
     * not public to force security checks, use K8sService instead.
     */
    static CompletableFuture<List<V1APIResource>> getResourceTypesAsync(CoreV1Api coreApi) {

        CompletableFuture<List<V1APIResource>> future = new CompletableFuture<>();
        try {
            coreApi.getAPIResourcesAsync(new ApiCallback<V1APIResourceList>() {
                @Override
                public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) { }

                @Override
                public void onSuccess(V1APIResourceList result, int statusCode, Map<String, List<String>> responseHeaders) {
                    LinkedList<V1APIResource> types = new LinkedList<>();
                    types.addAll(result.getResources().stream().collect(Collectors.toList()));
                    // add static resources
                    for (K8s r : K8s.values()) {
                        try {
                            types.stream().filter(t -> Objects.equals(t.getKind(), r.kind())).findFirst().or(() -> {
                                types.add(K8s.toResource(r));
                                return Optional.empty();
                            });
                        } catch (Exception e) {
                            LOGGER.error("Error adding resource type", e);
                        }
                    }

                    types.removeIf(t -> t.getKind().equals("Event")); // no usage for this

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

    public static String toResourceTypeString(V1APIResource r) {
        if (r == null) return null;
        if (isEmpty(r.getGroup()) && isEmpty(r.getVersion()))
            return r.getName();
        if (isEmpty(r.getGroup()))
            return r.getVersion() + "/" + r.getName();
        return r.getGroup() + "/" + r.getVersion() + "/" + r.getName();
    }

    public static String toYaml(KubernetesObject resource) {

        if (resource == null) return "";
        if (resource instanceof GenericObject) {
            Gson gson = new JSON().getGson();
            JsonElement jsonElement = gson.toJsonTree(((GenericObject)resource).toJson());
            String jsonTxt = gson.toJson(jsonElement);
            return jsonTxt; //TODO
        }
        // get yaml

        var resContent = Yaml.dump(resource);
        return resContent;
    }

    public static String getAge(OffsetDateTime creationTimestamp) {
        if (creationTimestamp == null) return "";
        final var age = System.currentTimeMillis()/1000 - creationTimestamp.toEpochSecond();
        if (age < 60) return age + "s";
        if (age < 3600) return age/60 + "m";
        if (age < 86400) return age/3600 + "h";
        return age/86400 + "d";
    }


    public static String getDns(V1Pod pod) {
        return pod.getMetadata().getName() + "." + pod.getMetadata().getNamespace() + ".cluster.local";
    }

    public static String getDns(V1Service service) {
        return service.getMetadata().getName() + "." + service.getMetadata().getNamespace() + ".svc.cluster.local";
    }
}
