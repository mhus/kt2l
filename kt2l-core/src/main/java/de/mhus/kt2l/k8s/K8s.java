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

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
        return Arrays.stream(RESOURCE.values()).filter(r -> r.resourceType().equals(resourceType)).findFirst()
                .orElseThrow(() -> new NotFoundRuntimeException("Unknown resource type: " + resourceType));
    }

    public enum RESOURCE {

        POD("pods","Pod",null,"v1","pod","po","", true),
        NODE("nodes","Node",null,"v1","node","no","", false),
        NAMESPACE("namespaces","Namespace",null,"v1","namespace","ns","", false),
        CONTAINER("containers","Container",null,"v1","container","co","", true),
        CONFIG_MAP("configmaps","ConfigMap",null,"v1","configmap","cm","", true),
        DEPLOYMENT("deployments","Deployment","apps","v1","deployment","deploy","", true),
        STATEFUL_SET("statefulsets","StatefulSet","apps","v1","statefulset","sts","", true),
        DAEMON_SET("daemonsets","DaemonSet","apps","v1","daemonset","ds","", true),
        REPLICA_SET("replicasets","ReplicaSet","apps","v1","replicaset","rs","", true),
        JOB("jobs","Job","batch","v1","job","job","", true),
        CRON_JOB("cronjobs","CronJob","batch","v1","cronjob","cj","", true),
        SECRET("secrets","Secret",null,"v1","secret","se","", true),
        SERVICE("services","Service",null,"v1","service","svc","", true),
        INGRESS("ingresses","Ingress","networking.k8s.io","v1","ingress","ing","", true),
        NETWORK_POLICY("networkpolicies","NetworkPolicy","networking.k8s.io","v1","networkpolicy","np","", true),
        PERSISTENT_VOLUME("persistentvolumes","PersistentVolume",null,"v1","persistentvolume","pv","", false),
        PERSISTENT_VOLUME_CLAIM("persistentvolumeclaims","PersistentVolumeClaim",null,"v1","persistentvolumeclaim","pvc","", true),
        STORAGE_CLASS("storageclasses","StorageClass","storage.k8s.io","v1","storageclass","sc","", false),
        SERVICE_ACCOUNT("serviceaccounts","ServiceAccount",null,"v1","serviceaccount","sa","", true),
        ROLE("roles","Role","rbac.authorization.k8s.io","v1","role","ro","", true),
        ROLE_BINDING("rolebindings","RoleBinding","rbac.authorization.k8s.io","v1","rolebinding","rb","", true),
        CLUSTER_ROLE("clusterroles","ClusterRole","rbac.authorization.k8s.io","v1","clusterrole","cr","", false),
        CLUSTER_ROLE_BINDING("clusterrolebindings","ClusterRoleBinding","rbac.authorization.k8s.io","v1","clusterrolebinding","crb","", false),
        CUSTOM_RESOURCE_DEFINITION("customresourcedefinitions","CustomResourceDefinition","apiextensions.k8s.io","","v1","crd", "", false),
        HPA("horizontalpodautoscalers","HorizontalPodAutoscaler","autoscaling","","v1","hpa", "", true),
        LIMIT_RANGE("limitranges","LimitRange",null,"v1","limitrange","lr","", true),
        GENERIC("","","","","","", "", false),
        CUSTOM("","","","","","", "", false);

        private final String resourceType;
        private final String kind;
        private final String group;
        private final String version;
        private final String singular;
        private final String shortNames;
        private final String categories;
        private final boolean namespaced;

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



        private RESOURCE(String resourceType, String  kind, String  group, String  version, String  singular, String  shortNames, String  categories, boolean namespaced) {
            this.resourceType = resourceType;
            this.kind = kind;
            this.group = group;
            this.version = version;
            this.singular = singular;
            this.shortNames = shortNames;
            this.categories = categories;
            this.namespaced = namespaced;
        }
    }

    public static final String NAMESPACE_ALL = "[all]";


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

    /**
     * not public to force security checks, use K8sService instead.
     */
    static CompletableFuture<List<V1APIResource>> getResourceTypesAsync(CoreV1Api coreApi) {

        CompletableFuture<List<V1APIResource>> future = new CompletableFuture<>();
        try {
            coreApi.getAPIResources().executeAsync(new ApiCallback<V1APIResourceList>() {
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

    public static RESOURCE toResourceType(V1APIResource resource) {
        if (resource == null)
            return null;
        return Arrays.stream(RESOURCE.values()).filter(r -> r.kind().equals(resource.getKind())).findFirst().orElseThrow(() -> new NotFoundRuntimeException("Unknown resource type: " + resource.getKind()));
    }

    static V1APIResource findResource(RESOURCE resource, List<V1APIResource> resources) {
        return resources.stream().filter(r ->  toResourceTypeString(r).endsWith(resource.resourceType())).findFirst().orElse(null);
    }

    static String toResourceTypeString(V1APIResource r) {
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
