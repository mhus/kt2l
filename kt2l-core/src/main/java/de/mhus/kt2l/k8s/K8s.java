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

import de.mhus.commons.tools.MCollection;
import de.mhus.commons.util.SoftHashMap;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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


    public final static V1APIResource POD = new V1APIResource().kind("Pod").name("pods").version("v1").singularName("pod").namespaced(true).shortNames(List.of("po"));
    public final static V1APIResource NODE = new V1APIResource().kind("Node").name("nodes").version("v1").singularName("node").namespaced(false).shortNames(List.of("no"));
    public final static V1APIResource NAMESPACE = new V1APIResource().kind("Namespace").name("namespaces").version("v1").singularName("namespace").namespaced(false).shortNames(List.of("ns"));
    public final static V1APIResource CONTAINER = new V1APIResource().kind("Container").name("containers").version("v1").singularName("container").namespaced(true).shortNames(List.of("co"));
    public final static V1APIResource CONFIG_MAP = new V1APIResource().kind("ConfigMap").name("configmaps").version("v1").singularName("configmap").namespaced(true).shortNames(List.of("cm"));
    public final static V1APIResource DEPLOYMENT = new V1APIResource().kind("Deployment").name("deployments").version("v1").group("apps").singularName("deployment").namespaced(true).shortNames(List.of("dep"));
    public final static V1APIResource STATEFUL_SET = new V1APIResource().kind("StatefulSet").name("statefulsets").version("v1").group("apps").singularName("statefulset").namespaced(true).shortNames(List.of("sts"));
    public final static V1APIResource DAEMON_SET = new V1APIResource().kind("DaemonSet").name("daemonsets").version("v1").group("apps").singularName("daemonset").namespaced(true).shortNames(List.of("ds"));
    public final static V1APIResource REPLICA_SET = new V1APIResource().kind("ReplicaSet").name("replicasets").version("v1").group("apps").singularName("replicaset").namespaced(true).shortNames(List.of("rs"));
    public final static V1APIResource JOB = new V1APIResource().kind("Job").name("jobs").version("v1").group("batch").singularName("job").namespaced(true);
    public final static V1APIResource CRON_JOB = new V1APIResource().kind("CronJob").name("cronjobs").version("v1").group("batch").singularName("cronjob").namespaced(true).shortNames(List.of("cj"));
    public final static V1APIResource SECRET = new V1APIResource().kind("Secret").name("secrets").version("v1").singularName("secret").namespaced(true).shortNames(List.of("se"));
    public final static V1APIResource SERVICE = new V1APIResource().kind("Service").name("services").version("v1").singularName("service").namespaced(true).shortNames(List.of("svc"));
    public final static V1APIResource INGRESS = new V1APIResource().kind("Ingress").name("ingresses").version("v1").group("networking.k8s.io").singularName("ingress").namespaced(true).shortNames(List.of("ing"));
    public final static V1APIResource NETWORK_POLICY = new V1APIResource().kind("NetworkPolicy").name("networkpolicies").version("v1").group("networking.k8s.io").singularName("networkpolicy").namespaced(true).shortNames(List.of("np"));
    public final static V1APIResource PERSISTENT_VOLUME = new V1APIResource().kind("PersistentVolume").name("persistentvolumes").version("v1").singularName("persistentvolume").namespaced(false).shortNames(List.of("pv"));
    public final static V1APIResource PERSISTENT_VOLUME_CLAIM = new V1APIResource().kind("PersistentVolumeClaim").name("persistentvolumeclaims").version("v1").singularName("persistentvolumeclaim").namespaced(true).shortNames(List.of("pvc"));
    public final static V1APIResource STORAGE_CLASS = new V1APIResource().kind("StorageClass").name("storageclasses").version("v1").group("storage.k8s.io").singularName("storageclass").namespaced(false).shortNames(List.of("sc"));
    public final static V1APIResource SERVICE_ACCOUNT = new V1APIResource().kind("ServiceAccount").name("serviceaccounts").version("v1").singularName("serviceaccount").namespaced(true).shortNames(List.of("sa"));
    public final static V1APIResource ROLE = new V1APIResource().kind("Role").name("roles").version("v1").group("rbac.authorization.k8s.io").singularName("role").namespaced(true).shortNames(List.of("ro"));
    public final static V1APIResource ROLE_BINDING = new V1APIResource().kind("RoleBinding").name("rolebindings").version("v1").group("rbac.authorization.k8s.io").singularName("rolebinding").namespaced(true).shortNames(List.of("rb"));
    public final static V1APIResource CLUSTER_ROLE = new V1APIResource().kind("ClusterRole").name("clusterroles").version("v1").group("rbac.authorization.k8s.io").singularName("clusterrole").namespaced(false).shortNames(List.of("cr"));
    public final static V1APIResource CLUSTER_ROLE_BINDING = new V1APIResource().kind("ClusterRoleBinding").name("clusterrolebindings").version("v1").group("rbac.authorization.k8s.io").singularName("clusterrolebinding").namespaced(false).shortNames(List.of("crb"));
    public final static V1APIResource CUSTOM_DEFINITION = new V1APIResource().kind("CustomResourceDefinition").name("customresourcedefinitions").version("apiextensions.k8s.io").singularName("crd").namespaced(false);
    public final static V1APIResource HPA = new V1APIResource().kind("HorizontalPodAutoscaler").name("horizontalpodautoscalers").version("v1").group("autoscaling").singularName("horizontalpodautoscaler").namespaced(true).shortNames(List.of("hpa"));
    public final static V1APIResource LIMIT_RANGE = new V1APIResource().kind("LimitRange").name("limitranges").version("v1").singularName("limitrange").namespaced(true).shortNames(List.of("lr"));
    public final static V1APIResource ENDPOINTS = new V1APIResource().kind("Endpoints").name("endpoints").version("v1").singularName("endpoints").namespaced(true).shortNames(List.of("ep"));

    private final static List<V1APIResource> resources = List.of(
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
            CUSTOM_DEFINITION,
            HPA,
            LIMIT_RANGE,
            ENDPOINTS
    );

    private static SoftHashMap<V1APIResource, String> DISPLAY_NAME_CACHE = new SoftHashMap<>();

    private static boolean equalsResource(V1APIResource res, String resourcePlural, String version) {
        return res.getName().equals(resourcePlural) && res.getVersion().equals(version);
    }

    public static V1APIResource toType(Discovery.APIResource r, String version) {
        return resources.stream().filter(res -> equalsResource(res, r.getResourcePlural(), version)).findFirst().orElseGet(
            () -> new V1APIResource()
                    .kind(r.getKind())
                    .name(r.getResourcePlural())
                    .version(version)
                    .namespaced(r.getNamespaced())
                    .singularName(r.getResourceSingular())
                    .group(r.getGroup())
        );
    }

    public static String displayName(V1APIResource res) {
        if (res == null) return "";
        return DISPLAY_NAME_CACHE.getOrCreate(res, k -> {
            var ds = (isEmpty(k.getGroup()) ? "" : k.getGroup() + ".") +
                     (isEmpty(k.getSingularName()) ? k.getName() : k.getSingularName()) +
                     (k.getVersion() == null ? "" : "_" + k.getVersion());
            return DISPLAY_MAPPER.getOrDefault(ds, ds);
        });
    }

    public static List<V1APIResource> resources() {
            return resources;
    }

    private static Map<Class<? extends KubernetesObject>, V1APIResource> classToResource = MCollection.asMap(
        V1Pod.class, POD,
        V1Node.class, NODE,
        V1Namespace.class, NAMESPACE,
        ContainerResource.class, CONTAINER,
        V1ConfigMap.class, CONFIG_MAP,
        V1Deployment.class, DEPLOYMENT,
        V1StatefulSet.class, STATEFUL_SET,
        V1DaemonSet.class, DAEMON_SET,
        V1ReplicaSet.class, REPLICA_SET,
        V1Job.class, JOB,
        V1CronJob.class, CRON_JOB,
        V1Secret.class, SECRET,
        V1Service.class, SERVICE,
        V1Ingress.class, INGRESS,
        V1NetworkPolicy.class, NETWORK_POLICY,
        V1PersistentVolume.class, PERSISTENT_VOLUME,
        V1PersistentVolumeClaim.class, PERSISTENT_VOLUME_CLAIM,
        V1StorageClass.class, STORAGE_CLASS,
        V1ServiceAccount.class, SERVICE_ACCOUNT,
        V1Role.class, ROLE,
        V1RoleBinding.class, ROLE_BINDING,
        V1ClusterRole.class, CLUSTER_ROLE,
        V1ClusterRoleBinding.class, CLUSTER_ROLE_BINDING,
        V1CustomResourceDefinition.class, CUSTOM_DEFINITION,
        V1HorizontalPodAutoscaler.class, HPA,
        V1LimitRange.class, LIMIT_RANGE,
        V1Endpoints.class, ENDPOINTS
    );

    public static Optional<V1APIResource> toResource(Class<? extends KubernetesObject> clazz) {
        return Optional.ofNullable(classToResource.get(clazz));
    }

    public static Class<? extends KubernetesObject> toClass(V1APIResource resource) {
        return classToResource.entrySet().stream().filter(e -> e.getValue().equals(resource)).map(Map.Entry::getKey).findFirst().orElse(null);
    }
}
