package de.mhus.kt2l.k8s;

import de.mhus.kt2l.resources.generic.GenericObject;
import de.mhus.kt2l.resources.pod.ContainerResource;
import io.kubernetes.client.common.KubernetesObject;
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

public enum K8s {

    POD("pods", "Pod", null, "v1", "pod", "po", "", true, V1Pod.class),
    NODE("nodes", "Node", null, "v1", "node", "no", "", false, V1Node.class),
    NAMESPACE("namespaces", "Namespace", null, "v1", "namespace", "ns", "", false, V1Namespace.class),
    CONTAINER("containers", "Container", null, "v1", "container", "co", "", true, ContainerResource.class),
    CONFIG_MAP("configmaps", "ConfigMap", null, "v1", "configmap", "cm", "", true, V1ConfigMap.class),
    DEPLOYMENT("deployments", "Deployment", "apps", "v1", "deployment", "deploy", "", true, V1Deployment.class),
    STATEFUL_SET("statefulsets", "StatefulSet", "apps", "v1", "statefulset", "sts", "", true, V1StatefulSet.class),
    DAEMON_SET("daemonsets", "DaemonSet", "apps", "v1", "daemonset", "ds", "", true, V1DaemonSet.class),
    REPLICA_SET("replicasets", "ReplicaSet", "apps", "v1", "replicaset", "rs", "", true, V1ReplicaSet.class),
    JOB("jobs", "Job", "batch", "v1", "job", "job", "", true, V1Job.class),
    CRON_JOB("cronjobs", "CronJob", "batch", "v1", "cronjob", "cj", "", true, V1CronJob.class),
    SECRET("secrets", "Secret", null, "v1", "secret", "se", "", true, V1Secret.class),
    SERVICE("services", "Service", null, "v1", "service", "svc", "", true, V1Service.class),
    INGRESS("ingresses", "Ingress", "networking.k8s.io", "v1", "ingress", "ing", "", true, V1Ingress.class),
    NETWORK_POLICY("networkpolicies", "NetworkPolicy", "networking.k8s.io", "v1", "networkpolicy", "np", "", true, V1NetworkPolicy.class),
    PERSISTENT_VOLUME("persistentvolumes", "PersistentVolume", null, "v1", "persistentvolume", "pv", "", false, V1PersistentVolume.class),
    PERSISTENT_VOLUME_CLAIM("persistentvolumeclaims", "PersistentVolumeClaim", null, "v1", "persistentvolumeclaim", "pvc", "", true, V1PersistentVolumeClaim.class),
    STORAGE_CLASS("storageclasses", "StorageClass", "storage.k8s.io", "v1", "storageclass", "sc", "", false, V1StorageClass.class),
    SERVICE_ACCOUNT("serviceaccounts", "ServiceAccount", null, "v1", "serviceaccount", "sa", "", true, V1ServiceAccount.class),
    ROLE("roles", "Role", "rbac.authorization.k8s.io", "v1", "role", "ro", "", true, V1Role.class),
    ROLE_BINDING("rolebindings", "RoleBinding", "rbac.authorization.k8s.io", "v1", "rolebinding", "rb", "", true, V1RoleBinding.class),
    CLUSTER_ROLE("clusterroles", "ClusterRole", "rbac.authorization.k8s.io", "v1", "clusterrole", "cr", "", false, V1ClusterRole.class),
    CLUSTER_ROLE_BINDING("clusterrolebindings", "ClusterRoleBinding", "rbac.authorization.k8s.io", "v1", "clusterrolebinding", "crb", "", false, V1ClusterRoleBinding.class),
    CUSTOM_RESOURCE_DEFINITION("customresourcedefinitions", "CustomResourceDefinition", "apiextensions.k8s.io", "", "v1", "crd", "", false, V1CustomResourceDefinition.class),
    HPA("horizontalpodautoscalers", "HorizontalPodAutoscaler", "autoscaling", "", "v1", "hpa", "", true, V1HorizontalPodAutoscaler.class),
    LIMIT_RANGE("limitranges", "LimitRange", null, "v1", "limitrange", "lr", "", true, V1LimitRange.class),
    ENDPOINTS("endpoints", "Endpoints", null, "v1", "endpoints", "ep", "", true, V1Endpoints.class),
    GENERIC("", "", "", "", "", "", "", false, GenericObject.class),
    CUSTOM("", "", "", "", "", "", "", false, KubernetesObject.class);

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


    private K8s(String resourceType, String kind, String group, String version, String singular, String shortNames, String categories, boolean namespaced, Class<? extends KubernetesObject> clazz) {
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
