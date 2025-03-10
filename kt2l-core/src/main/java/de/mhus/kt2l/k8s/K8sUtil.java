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

import de.mhus.commons.console.ConsoleTable;
import de.mhus.commons.errors.NotFoundRuntimeException;
import de.mhus.commons.tools.MJson;
import de.mhus.commons.yaml.MYaml;
import de.mhus.kt2l.aaa.AaaConfiguration;
import de.mhus.kt2l.aaa.SecurityService;
import de.mhus.kt2l.cluster.Cluster;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.util.Yaml;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static de.mhus.commons.tools.MLang.tryThis;
import static de.mhus.commons.tools.MString.isEmpty;

@Slf4j
public class K8sUtil {

    public static final String WATCH_EVENT_ADDED = "ADDED";
    public static final String WATCH_EVENT_MODIFIED = "MODIFIED";
    public static final String WATCH_EVENT_DELETED = "DELETED";
    public static final String NAMESPACE_DEFAULT = "default";
    public static final String NAMESPACE_ALL_LABEL = "[all]";
    public static final String NAMESPACE_ALL = "*";

    public static V1APIResource toType(String resourceType) {
        if (isEmpty(resourceType))
            throw new NullPointerException("Resource type is empty");
        return K8s.resources().stream().filter(r -> r.getName().equals(resourceType) || r.getKind().equals(resourceType)).findFirst()
                .orElseThrow(() -> new NotFoundRuntimeException("Unknown resource type: " + resourceType));
    }

    public static V1APIResource toType(KubernetesObject o, Cluster cluster) {
        if (cluster.getTypes() == null)
            throw new IllegalArgumentException("Types not found in cluster configuration");
        var resource = K8s.toResource(o.getClass()).orElse(null);
        return resource;
    }

    // This is not possible in any cases
//    public static V1APIResource toResource(KubernetesObject o, Cluster cluster) {
//        if (cluster.getResourceTypes() == null)
//            throw new IllegalArgumentException("ResourceTypes not found in cluster configuration");
//
//        try {
//            var kind = o.getKind();
//            if (kind != null) {
//                var resource = cluster.getResourceTypes().stream().filter(r -> r.clazz().equals(o.getClass())).findFirst().orElse(null);
//                if (resource != null) return K8s.toResource(resource);
//            }
//        } catch (Exception e) {
//            LOGGER.debug("Error getting resource type for {} {}", o.getClass(), e.getMessage());
//        }
//
//        if (o instanceof DynamicKubernetesObject dynamicKubernetesObject) {
//            var kind = dynamicKubernetesObject.getRaw().get("kind");
//            if (kind != null && cluster != null) {
//                var resource = cluster.getResourceTypes().stream().filter(r -> r.kind().equals(kind)).findFirst().orElse(null);
//                if (resource != null) return K8s.toResource(resource);
//            }
//        }
//
//        if (cluster == null) {
//            var resource = Arrays.stream(K8s.values()).filter(r -> r.clazz().equals(o.getClass())).findFirst().orElse(null);
//            if (resource != null) return K8s.toResource(resource);
//        } else {
//            var resource = cluster.getResourceTypes().stream().filter(r -> r.clazz().equals(o.getClass())).findFirst().orElse(null);
//            if (resource != null) return K8s.toResource(resource);
//        }
//
//        throw new IllegalArgumentException("Kind not found in cluster for " + o.getClass().getSimpleName());
//    }

    public static void describeHeader(ApiProvider apiProvider, HandlerK8s handler, KubernetesObject res, StringBuilder sb) {
        var kind = tryThis(() -> res.getKind()).orElse(null);
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
            sb.append("Created:       ").append(creationTimestamp).append(" (Age: ").append(getAge(creationTimestamp)).append(")").append("\n");
        }
        var deletionTimestamp = res.getMetadata().getDeletionTimestamp();
        if (deletionTimestamp != null) {
            sb.append("Deletion:      ").append(deletionTimestamp).append(" (In: ").append(getAge(deletionTimestamp)).append(")").append("\n");
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

    public static void describeFooter(ApiProvider apiProvider, HandlerK8s handler, KubernetesObject res, StringBuilder sb) {

        try {
            final var uid = res.getMetadata().getUid();
            final var namespace = res.getMetadata().getNamespace();
            final var fieldSelector = "involvedObject.uid=" + uid;
            final var list = namespace == null ?
                    apiProvider.getCoreV1Api().listEventForAllNamespaces().fieldSelector(fieldSelector).limit(10).execute()
                    :
                    apiProvider.getCoreV1Api().listNamespacedEvent( namespace).fieldSelector(fieldSelector).limit(10).execute();

            final var events = list.getItems();
            if (events != null && events.size() > 0) {
                sb.append("\nEvents:\n\n");
                ConsoleTable table = new ConsoleTable();
                table.setFull(true);
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

    /**
     * not public to force security checks, use K8sService instead.
     */
    static List<String> getNamespaces(CoreV1Api coreApi) {
        LinkedList<String> namespaces = new LinkedList<>();
        try {
            coreApi.listNamespace().execute()
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
            coreApi.listNamespace().executeAsync(new ApiCallback<V1NamespaceList>() {
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
            coreApi.getAPIResources().execute().getResources().forEach(res -> types.add(res));
        } catch (ApiException e) {
            LOGGER.error("Error getting resource types", e);
        }
        return types;
    }

    public static String toYamlString(KubernetesObject resource) {

        if (resource == null) return "";
        // get yaml
        try {
            if (resource instanceof DynamicKubernetesObject dynamicKubernetesObject) {
                String jsonTxt = dynamicKubernetesObject.getRaw().toString();
                return MYaml.toYaml(MJson.load(jsonTxt)).toString();
            }
            var resContent = Yaml.dump(resource);
            return resContent;
        } catch (Exception e) {
            LOGGER.debug("Error converting to yaml", e);
            return "ERROR: " + e.getMessage() + "\n" + resource.toString();
        }
    }

    public static String getAge(OffsetDateTime creationTimestamp) {
        if (creationTimestamp == null || creationTimestamp.toEpochSecond() == 0) return "";
        var age = System.currentTimeMillis()/1000 - creationTimestamp.toEpochSecond();
        if (age < 0) age = -age;
        if (age < 120) return age + "s";
        if (age < 3600*2) return age/60 + "m";
        if (age < 86400*2) return age/3600 + "h";
        if (age < 86400*1000) return age/86400 + "d";
        return age/86400/365 + "y";
    }

    public static String getAge(long age) {
        if (age < 0) age = -age;
        if (age == 0) return "0";
        if (age < 120) return age + "s";
        if (age < 3600*2) return age/60 + "m";
        if (age < 86400*2) return age/3600 + "h";
        if (age < 86400*1000) return age/86400 + "d";
        return age/86400/365 + "y";
    }

    public static String getDns(V1Pod pod) {
        return pod.getMetadata().getName() + "." + pod.getMetadata().getNamespace() + ".cluster.local";
    }

    public static String getDns(V1Service service) {
        return service.getMetadata().getName() + "." + service.getMetadata().getNamespace() + ".svc.cluster.local";
    }

    public static void checkDeleteAccess(SecurityService securityService, V1APIResource resource) throws ApiException {
        var defaultRole = securityService.getRolesForResource(AaaConfiguration.SCOPE_DEFAULT, AaaConfiguration.SCOPE_RESOURCE_DELETE);
        if (!securityService.hasRole(AaaConfiguration.SCOPE_RESOURCE_DELETE, resource.getName(), defaultRole))
            throw new ApiException(403, "Access denied for non admin users");
    }

    public static boolean matchLabels(Map<String, String> selector, Map<String, String> labels) {
        if (selector == null || labels == null) return false;
        for (var e : selector.entrySet()) {
            if (!labels.containsKey(e.getKey())) return false;
            if (!labels.get(e.getKey()).equals(e.getValue())) return false;
        }
        return true;
    }

    static Map<String, Object> findObject(ArrayList<Object> list, String name) {
        if (list == null) {
            return null;
        } else {
            Iterator var2 = list.iterator();

            Map map;
            do {
                if (!var2.hasNext()) {
                    return null;
                }

                Object obj = var2.next();
                map = (Map)obj;
            } while(!name.equals(map.get("name")));

            return map;
        }
    }

    public static int compareTo(OffsetDateTime a, OffsetDateTime b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }

    public static V1ContainerStatus findDefaultContainer(V1Pod pod) {
        if (pod.getStatus().getContainerStatuses() != null) {
            for (V1ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                if (cs.getState().getTerminated() == null && cs.getState().getRunning() != null) return cs;
            }
        }
        if (pod.getStatus().getEphemeralContainerStatuses() != null) {
            for (V1ContainerStatus cs : pod.getStatus().getEphemeralContainerStatuses()) {
                if (cs.getState().getTerminated() == null && cs.getState().getRunning() != null) return cs;
            }
        }
        if (pod.getStatus().getInitContainerStatuses() != null) {
            for (V1ContainerStatus cs : pod.getStatus().getInitContainerStatuses()) {
                if (cs.getState().getTerminated() == null && cs.getState().getRunning() != null) return cs;
            }
        }
        return null;
    }

    public static String getAttachableContainer(V1Pod pod) {
        if (pod.getStatus().getContainerStatuses() != null) {
            for (V1ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                if (cs.getState().getTerminated() == null && cs.getState().getRunning() != null) return cs.getName();
            }
        }
        if (pod.getStatus().getEphemeralContainerStatuses() != null) {
            for (V1ContainerStatus cs : pod.getStatus().getEphemeralContainerStatuses()) {
                if (cs.getState().getTerminated() == null && cs.getState().getRunning() != null) return cs.getName();
            }
        }
        if (pod.getStatus().getInitContainerStatuses() != null) {
            for (V1ContainerStatus cs : pod.getStatus().getInitContainerStatuses()) {
                if (cs.getState().getTerminated() == null && cs.getState().getRunning() != null) return cs.getName();
            }
        }
        return null;
    }

    public static boolean hasTty(V1Pod pod, String containerName) {
        if (pod.getSpec().getContainers() != null)
            for (var c : pod.getSpec().getContainers()) {
                if (c.getName().equals(containerName)) {
                    return c.getTty() != null && c.getTty();
                }
            }
        if (pod.getSpec().getInitContainers() != null)
            for (var c : pod.getSpec().getInitContainers()) {
                if (c.getName().equals(containerName)) {
                    return c.getTty() != null && c.getTty();
                }
            }
        if (pod.getSpec().getEphemeralContainers() != null)
            for (var c : pod.getSpec().getEphemeralContainers()) {
                if (c.getName().equals(containerName)) {
                    return c.getTty() != null && c.getTty();
                }
            }
        return false;
    }

    public static String normalizeLabelKey(String key) {
        if (key == null) throw new NullPointerException("Key is null");
        return key.replaceAll("![A-Za-z0-9_\\-\\./]", "_");
    }

    public static String normalizeLabelValue(String key) {
        if (key == null) throw new NullPointerException("Key is null");
        return key.replaceAll("![A-Za-z0-9_\\-\\./]", "_");
    }

    public static String normalizeAnnotationKey(String key) {
        if (key == null) throw new NullPointerException("Key is null");
        return key.replaceAll("![A-Za-z0-9_\\-\\./]", "_");
    }

    public static String normalizeAnnotationValue(String key) {
        if (key == null) throw new NullPointerException("Key is null");
        return key.replaceAll("![A-Za-z0-9_\\-\\./]", "_");
    }

}
