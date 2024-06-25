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

import de.mhus.commons.errors.NotFoundRuntimeException;
import de.mhus.commons.tools.MCollection;
import de.mhus.kt2l.resources.pod.ContainerResource;
import io.kubernetes.client.Discovery;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
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
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static de.mhus.commons.tools.MString.isEmpty;

public class K8s {

    private static final Map<String,String> DISPLAY_MAPPER = Collections.synchronizedMap(
            MCollection.asMap(
                    "pod_v1", "pod",
                    "node_v1", "node",
                    "namespace_v1", "namespace",
                    "container_v1", "container",
                    "configmap_v1", "configmap",
                    "apps.deployment_v1", "deployment",
                    "apps.statefulset_v1", "statefulset",
                    "apps.daemonset_v1", "daemonset",
                    "apps.replicaset_v1", "replicaset",
                    "batch.job_v1", "job",
                    "batch.cronjob_v1", "cronjob",
                    "rbac.authorization.k8s.io.role_v1", "role",
                    "rbac.authorization.k8s.io.rolebinding_v1", "rolebinding",
                    "rbac.authorization.k8s.io.clusterrole_v1", "clusterrole",
                    "rbac.authorization.k8s.io.clusterrolebinding_v1", "clusterrolebinding",
                    "serviceaccount_v1", "serviceaccount",
                    "secret_v1", "secret",
                    "service_v1", "service",
                    "networking.k8s.io.ingress_v1", "ingress",
                    "networking.k8s.io.networkpolicy_v1", "networkpolicy",
                    "persistentvolume_v1", "persistentvolume",
                    "persistentvolumeclaim_v1", "persistentvolumeclaim",
                    "storage.k8s.io.storageclass_v1", "storageclass",
                    "autoscaling.horizontalpodautoscaler_v1", "horizontalpodautoscaler",
                    "limitrange", "limitrange",
                    "endpoints_v1", "endpoints"
            ));

    public final static K8s POD = new K8s("pods", "Pod", null, "v1", "pod", "po", "", true, V1Pod.class);
    public final static K8s NODE = new K8s("nodes", "Node", null, "v1", "node", "no", "", false, V1Node.class);
    public final static K8s NAMESPACE = new K8s("namespaces", "Namespace", null, "v1", "namespace", "ns", "", false, V1Namespace.class);
    public final static K8s CONTAINER = new K8s("containers", "Container", null, "v1", "container", "co", "", true, ContainerResource.class);
    public final static K8s CONFIG_MAP = new K8s("configmaps", "ConfigMap", null, "v1", "configmap", "cm", "", true, V1ConfigMap.class);
    public final static K8s DEPLOYMENT = new K8s("deployments", "Deployment", "apps", "v1", "deployment", "deploy", "", true, V1Deployment.class);
    public final static K8s STATEFUL_SET = new K8s("statefulsets", "StatefulSet", "apps", "v1", "statefulset", "sts", "", true, V1StatefulSet.class);
    public final static K8s DAEMON_SET = new K8s("daemonsets", "DaemonSet", "apps", "v1", "daemonset", "ds", "", true, V1DaemonSet.class);
    public final static K8s REPLICA_SET = new K8s("replicasets", "ReplicaSet", "apps", "v1", "replicaset", "rs", "", true, V1ReplicaSet.class);
    public final static K8s JOB = new K8s("jobs", "Job", "batch", "v1", "job", "job", "", true, V1Job.class);
    public final static K8s CRON_JOB = new K8s("cronjobs", "CronJob", "batch", "v1", "cronjob", "cj", "", true, V1CronJob.class);
    public final static K8s SECRET = new K8s("secrets", "Secret", null, "v1", "secret", "se", "", true, V1Secret.class);
    public final static K8s SERVICE = new K8s("services", "Service", null, "v1", "service", "svc", "", true, V1Service.class);
    public final static K8s INGRESS = new K8s("ingresses", "Ingress", "networking.k8s.io", "v1", "ingress", "ing", "", true, V1Ingress.class);
    public final static K8s NETWORK_POLICY = new K8s("networkpolicies", "NetworkPolicy", "networking.k8s.io", "v1", "networkpolicy", "np", "", true, V1NetworkPolicy.class);
    public final static K8s PERSISTENT_VOLUME = new K8s("persistentvolumes", "PersistentVolume", null, "v1", "persistentvolume", "pv", "", false, V1PersistentVolume.class);
    public final static K8s PERSISTENT_VOLUME_CLAIM = new K8s("persistentvolumeclaims", "PersistentVolumeClaim", null, "v1", "persistentvolumeclaim", "pvc", "", true, V1PersistentVolumeClaim.class);
    public final static K8s STORAGE_CLASS = new K8s("storageclasses", "StorageClass", "storage.k8s.io", "v1", "storageclass", "sc", "", false, V1StorageClass.class);
    public final static K8s SERVICE_ACCOUNT = new K8s("serviceaccounts", "ServiceAccount", null, "v1", "serviceaccount", "sa", "", true, V1ServiceAccount.class);
    public final static K8s ROLE = new K8s("roles", "Role", "rbac.authorization.k8s.io", "v1", "role", "ro", "", true, V1Role.class);
    public final static K8s ROLE_BINDING = new K8s("rolebindings", "RoleBinding", "rbac.authorization.k8s.io", "v1", "rolebinding", "rb", "", true, V1RoleBinding.class);
    public final static K8s CLUSTER_ROLE = new K8s("clusterroles", "ClusterRole", "rbac.authorization.k8s.io", "v1", "clusterrole", "cr", "", false, V1ClusterRole.class);
    public final static K8s CLUSTER_ROLE_BINDING = new K8s("clusterrolebindings", "ClusterRoleBinding", "rbac.authorization.k8s.io", "v1", "clusterrolebinding", "crb", "", false, V1ClusterRoleBinding.class);
    public final static K8s CUSTOM_RESOURCE_DEFINITION = new K8s("customresourcedefinitions", "CustomResourceDefinition", "apiextensions.k8s.io", "", "v1", "crd", "", false, V1CustomResourceDefinition.class);
    public final static K8s HPA = new K8s("horizontalpodautoscalers", "HorizontalPodAutoscaler", "autoscaling", "v1", "horizontalpodautoscaler", "hpa", "", true, V1HorizontalPodAutoscaler.class);
    public final static K8s LIMIT_RANGE = new K8s("limitranges", "LimitRange", null, "v1", "limitrange", "lr", "", true, V1LimitRange.class);
    public final static K8s ENDPOINTS = new K8s("endpoints", "Endpoints", null, "v1", "endpoints", "ep", "", true, V1Endpoints.class);
    public final static K8s GENERIC = new K8s("GENERIC", "GENERIC", "", "", "", "", "", false, DynamicKubernetesObject.class);

    public final static K8s[] values() {
        return new K8s[] {
            POD,
            NODE,
            NAMESPACE,
            CONTAINER,
            CONFIG_MAP,
            DEPLOYMENT,
            STATEFUL_SET,
            DAEMON_SET,
            REPLICA_SET,
            JOB,
            CRON_JOB,
            SECRET,
            SERVICE,
            INGRESS,
            NETWORK_POLICY,
            PERSISTENT_VOLUME,
            PERSISTENT_VOLUME_CLAIM,
            STORAGE_CLASS,
            SERVICE_ACCOUNT,
            ROLE,
            ROLE_BINDING,
            CLUSTER_ROLE,
            CLUSTER_ROLE_BINDING,
            CUSTOM_RESOURCE_DEFINITION,
            HPA,
            LIMIT_RANGE,
            ENDPOINTS,
        };
    }

    private final String plural;
    private final String kind;
    private final String group;
    private final String version;
    private final String singular;
    private final String shortNames;
    private final String categories;
    private final boolean namespaced;
    private final Class<? extends KubernetesObject> clazz;
    private final String displayName;

    public static V1APIResource toResource(K8s resource) {
        return new V1APIResource().kind(resource.kind()).name(resource.plural()).version(resource.version()).group(resource.group());
    }

    public static K8s toType(V1APIResource resource) {
        if (resource == null)
            return null;
        return Arrays.stream(values()).filter(r -> r.kind().equals(resource.getKind())).findFirst().orElseThrow(() -> new NotFoundRuntimeException("Unknown resource type: " + resource.getKind()));
    }

    public static String toResourceType(String group, String apiVersion, String plural) {
        if (isEmpty(group) && isEmpty(apiVersion))
            return plural;
        if (isEmpty(group))
            return apiVersion + "/" + plural;
        return group + "/" + apiVersion + "/" + plural;
    }

    public static K8s toResource(Discovery.APIResource r, String version) {
        return Arrays.stream(values()).filter(res -> res.equals(r.getResourceSingular(), version)).findFirst().orElseGet(
                () -> new K8s(r.getResourcePlural(), r.getKind(), r.getGroup(), version, r.getResourceSingular(), "", "", r.getNamespaced(), KubernetesObject.class)
        );
    }

    public boolean isNamespaced() {
        return namespaced;
    }

    public String plural() {
        return plural;
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


    private K8s(String plural, String kind, String group, String version, String singular, String shortNames, String categories, boolean namespaced, Class<? extends KubernetesObject> clazz) {
        this.plural = plural;
        this.kind = kind;
        this.group = group;
        this.version = version;
        this.singular = singular;
        this.shortNames = shortNames;
        this.categories = categories;
        this.namespaced = namespaced;
        this.clazz = clazz;
        var ds = (isEmpty(group) ? "" : group + ".") + singular + (version == null ? "" : "_" + version);
        this.displayName = DISPLAY_MAPPER.getOrDefault(ds, ds);
    }

    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof K8s)) {
            return false;
        }
        K8s other = (K8s) obj;
        return plural.equals(other.plural) &&
                kind.equals(other.kind) &&
                group.equals(other.group) &&
                version.equals(other.version) &&
                singular.equals(other.singular) &&
                namespaced == other.namespaced;
    }

    public int hashCode() {
        return Objects.hash(plural, version);
    }

    public String toString() {
        return plural;
    }

    public boolean equals(String name, String version) {
        return singular.equals(name) && this.version.equals(version);
    }

    public String displayName() {
        return displayName;
    }
}
