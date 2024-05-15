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
import de.mhus.commons.errors.NotFoundRuntimeException;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.resources.generic.GenericObject;
import de.mhus.kt2l.resources.pod.ContainerResource;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1APIResourceList;
import io.kubernetes.client.openapi.models.V1ClusterRole;
import io.kubernetes.client.openapi.models.V1ClusterRoleBinding;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1CronJob;
import io.kubernetes.client.openapi.models.V1CustomResourceDefinition;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Endpoints;
import io.kubernetes.client.openapi.models.V1HorizontalPodAutoscaler;
import io.kubernetes.client.openapi.models.V1Ingress;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1LimitRange;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1NetworkPolicy;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1Role;
import io.kubernetes.client.openapi.models.V1RoleBinding;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceAccount;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1StorageClass;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static de.mhus.commons.tools.MString.isEmpty;

@Slf4j
public class K8s {

    public static final String WATCH_EVENT_ADDED = "ADDED";
    public static final String WATCH_EVENT_MODIFIED = "MODIFIED";
    public static final String WATCH_EVENT_DELETED = "DELETED";

    public static RESOURCE toResourceType(String resourceType) {
        if (isEmpty(resourceType))
            throw new NullPointerException("Resource type is empty");
        return Arrays.stream(RESOURCE.values()).filter(r -> r.resourceType().equals(resourceType) || r.kind().equals(resourceType)).findFirst()
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
        var resource = Arrays.stream(RESOURCE.values()).filter(r -> r.clazz().equals(o.getClass())).findFirst().orElse(null);
        if (resource != null) return toResource(resource);

        throw new IllegalArgumentException("Kind not found in cluster: " + kind);
    }

    public static void previewHeader(ApiProvider apiProvider, HandlerK8s handler, KubernetesObject res, StringBuilder sb) {
        var kind = res.getKind();
        if (kind != null) {
            sb.append("Kind:      ").append(kind).append("\n");
        }
        var name = res.getMetadata().getName();
            sb.append("Name:      ").append(name).append("\n");
        var namespace = res.getMetadata().getNamespace();
        if (namespace != null) {
            sb.append("Namespace: ").append(namespace).append("\n");
        }
        var creationTimestamp = res.getMetadata().getCreationTimestamp();
        if (creationTimestamp != null) {
            sb.append("Created:   ").append(creationTimestamp).append("\n");
        }
    }

    public static void previewFooter(ApiProvider apiProvider, HandlerK8s handler, KubernetesObject res, StringBuilder sb) {
        

    }

    public enum RESOURCE {

        POD("pods","Pod",null,"v1","pod","po","", true, V1Pod.class),
        NODE("nodes","Node",null,"v1","node","no","", false, V1Node.class),
        NAMESPACE("namespaces","Namespace",null,"v1","namespace","ns","", false, V1Namespace.class),
        CONTAINER("containers","Container",null,"v1","container","co","", true, ContainerResource.class),
        CONFIG_MAP("configmaps","ConfigMap",null,"v1","configmap","cm","", true, V1ConfigMap.class),
        DEPLOYMENT("deployments","Deployment","apps","v1","deployment","deploy","", true, V1Deployment.class),
        STATEFUL_SET("statefulsets","StatefulSet","apps","v1","statefulset","sts","", true, V1StatefulSet.class),
        DAEMON_SET("daemonsets","DaemonSet","apps","v1","daemonset","ds","", true, V1DaemonSet.class),
        REPLICA_SET("replicasets","ReplicaSet","apps","v1","replicaset","rs","", true, V1ReplicaSet.class),
        JOB("jobs","Job","batch","v1","job","job","", true, V1Job.class),
        CRON_JOB("cronjobs","CronJob","batch","v1","cronjob","cj","", true, V1CronJob.class),
        SECRET("secrets","Secret",null,"v1","secret","se","", true, V1Secret.class),
        SERVICE("services","Service",null,"v1","service","svc","", true, V1Service.class),
        INGRESS("ingresses","Ingress","networking.k8s.io","v1","ingress","ing","", true, V1Ingress.class),
        NETWORK_POLICY("networkpolicies","NetworkPolicy","networking.k8s.io","v1","networkpolicy","np","", true, V1NetworkPolicy.class),
        PERSISTENT_VOLUME("persistentvolumes","PersistentVolume",null,"v1","persistentvolume","pv","", false, V1PersistentVolume.class),
        PERSISTENT_VOLUME_CLAIM("persistentvolumeclaims","PersistentVolumeClaim",null,"v1","persistentvolumeclaim","pvc","", true, V1PersistentVolumeClaim.class),
        STORAGE_CLASS("storageclasses","StorageClass","storage.k8s.io","v1","storageclass","sc","", false, V1StorageClass.class),
        SERVICE_ACCOUNT("serviceaccounts","ServiceAccount",null,"v1","serviceaccount","sa","", true, V1ServiceAccount.class),
        ROLE("roles","Role","rbac.authorization.k8s.io","v1","role","ro","", true, V1Role.class),
        ROLE_BINDING("rolebindings","RoleBinding","rbac.authorization.k8s.io","v1","rolebinding","rb","", true, V1RoleBinding.class),
        CLUSTER_ROLE("clusterroles","ClusterRole","rbac.authorization.k8s.io","v1","clusterrole","cr","", false, V1ClusterRole.class),
        CLUSTER_ROLE_BINDING("clusterrolebindings","ClusterRoleBinding","rbac.authorization.k8s.io","v1","clusterrolebinding","crb","", false, V1ClusterRoleBinding.class),
        CUSTOM_RESOURCE_DEFINITION("customresourcedefinitions","CustomResourceDefinition","apiextensions.k8s.io","","v1","crd", "", false, V1CustomResourceDefinition.class),
        HPA("horizontalpodautoscalers","HorizontalPodAutoscaler","autoscaling","","v1","hpa", "", true, V1HorizontalPodAutoscaler.class),
        LIMIT_RANGE("limitranges","LimitRange",null,"v1","limitrange","lr","", true, V1LimitRange.class),
        ENDPOINTS("endpoints","Endpoints",null,"v1","endpoints","ep","", true, V1Endpoints.class),
        GENERIC("","","","","","", "", false, GenericObject.class),
        CUSTOM("","","","","","", "", false, KubernetesObject.class);

        private final String resourceType;
        private final String kind;
        private final String group;
        private final String version;
        private final String singular;
        private final String shortNames;
        private final String categories;
        private final boolean namespaced;
        private final Class<? extends KubernetesObject> clazz;

        public boolean isNamespaced() {
            return namespaced;
        }

        public String resourceType() {
            return resourceType;
        }

        public String kind() {
            return kind;
        }

        public String group() {
            return group;
        }

        public String version() {
            return version;
        }

        public String singular() {
            return singular;
        }

        public String shortNames() {
            return shortNames;
        }

        public String categories() {
            return categories;
        }

        public Class<? extends KubernetesObject> clazz() {
            return clazz;
        }



        private RESOURCE(String resourceType, String  kind, String  group, String  version, String  singular, String  shortNames, String  categories, boolean namespaced, Class<? extends KubernetesObject> clazz) {
            this.resourceType = resourceType;
            this.kind = kind;
            this.group = group;
            this.version = version;
            this.singular = singular;
            this.shortNames = shortNames;
            this.categories = categories;
            this.namespaced = namespaced;
            this.clazz = clazz;
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
                    for (RESOURCE r : RESOURCE.values()) {
                        try {
                            types.stream().filter(t -> Objects.equals(t.getKind(), r.kind())).findFirst().or(() -> {
                                types.add(toResource(r));
                                return null;
                            });
                        } catch (Exception e) {
                            LOGGER.error("Error adding resource type", e);
                        }
                    }

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

    public static V1APIResource toResource(RESOURCE resource) {
        return new V1APIResource().kind(resource.kind()).name(resource.resourceType()).version(resource.version()).group(resource.group());
    }

    public static RESOURCE toResourceType(V1APIResource resource) {
        if (resource == null)
            return null;
        return Arrays.stream(RESOURCE.values()).filter(r -> r.kind().equals(resource.getKind())).findFirst().orElseThrow(() -> new NotFoundRuntimeException("Unknown resource type: " + resource.getKind()));
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


    public static String getDns(V1Pod pod) {
        return pod.getMetadata().getName() + "." + pod.getMetadata().getNamespace() + ".cluster.local";
    }

    public static String getDns(V1Service service) {
        return service.getMetadata().getName() + "." + service.getMetadata().getNamespace() + ".svc.cluster.local";
    }
}
