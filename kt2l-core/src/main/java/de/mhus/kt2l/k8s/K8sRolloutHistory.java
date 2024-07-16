package de.mhus.kt2l.k8s;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.extended.kubectl.util.deployment.DeploymentHelper;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1ControllerRevision;
import io.kubernetes.client.openapi.models.V1ControllerRevisionList;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.util.labels.LabelSelector;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class K8sRolloutHistory {

    public static final String CHANGE_CAUSE_ANNOTATION = "kubernetes.io/change-cause";

    private long revision;

    @Getter
    private final List<History> histories;
    @Setter
    private ApiClient apiClient;
    @Setter
    private Class<? extends KubernetesObject> apiTypeClass;
    @Setter
    private String name;
    @Setter
    private String namespace;

    public K8sRolloutHistory() {
        histories = new ArrayList<>();
        revision = 0;
    }

    public List<History> execute() throws ApiException, KubectlException {
        validate();
        AppsV1Api api = new AppsV1Api(this.apiClient);
        if (apiTypeClass.equals(V1Deployment.class)) {
            V1Deployment deployment = api.readNamespacedDeployment(name, namespace, null);
            deploymentViewHistory(deployment, api);
        } else if (apiTypeClass.equals(V1DaemonSet.class)) {
            V1DaemonSet daemonSet = api.readNamespacedDaemonSet(name, namespace, null);
            daemonSetViewHistory(daemonSet, api);
        } else if (apiTypeClass.equals(V1StatefulSet.class)) {
            V1StatefulSet statefulSet = api.readNamespacedStatefulSet(name, namespace, null);
            statefulSetViewHistory(statefulSet, api);
        } else {
            throw new ApiException("Unsupported class for rollout history: " + apiTypeClass);
        }
        return histories;
    }

    private void validate() throws ApiException {
        StringBuilder msg = new StringBuilder();
        if (name == null) {
            msg.append("Missing name, ");
        }
        if (namespace == null) {
            msg.append("Missing namespace, ");
        }
        if (revision < 0) {
            msg.append("revision must be a positive integer: ").append(revision);
        }
        if (msg.length() > 0) {
            throw new ApiException(msg.toString());
        }
    }

    private void deploymentViewHistory(V1Deployment deployment, AppsV1Api api) throws ApiException {
        List<V1ReplicaSet> allOldRSs = new ArrayList<>();
        List<V1ReplicaSet> oldRSs = new ArrayList<>();
        V1ReplicaSet newRs = DeploymentHelper.getAllReplicaSets(deployment, api, oldRSs, allOldRSs);
        if (newRs != null) {
            allOldRSs.add(newRs);
        }
        Map<Long, V1PodTemplateSpec> historyInfo = new HashMap<>();
        for (V1ReplicaSet rs : allOldRSs) {
            Long v = DeploymentHelper.revision(rs.getMetadata());
            historyInfo.put(v, rs.getSpec().getTemplate());
            String changeCause = getChangeCause(rs.getMetadata());
            if (historyInfo.get(v).getMetadata().getAnnotations() == null) {
                historyInfo.get(v).getMetadata().setAnnotations(new HashMap<>());
            }
            if (changeCause != null && changeCause.length() > 0) {
                historyInfo
                        .get(v)
                        .getMetadata()
                        .getAnnotations()
                        .put(CHANGE_CAUSE_ANNOTATION, changeCause);
            }
        }

        if (revision > 0) {
            return;
        }
        List<Long> revisions = new ArrayList<>(historyInfo.keySet());
        revisions.sort(Long::compareTo);
        for (Long revision : revisions) {
            String changeCause = getChangeCause(historyInfo.get(revision).getMetadata());
            if (changeCause == null || changeCause.isEmpty()) {
                changeCause = "<none>";
            }
            histories.add(new History(revision, changeCause));
        }
    }

    private void daemonSetViewHistory(V1DaemonSet daemonSet, AppsV1Api api)
            throws ApiException, KubectlException {
        LabelSelector selector = LabelSelector.parse(daemonSet.getSpec().getSelector());
        List<V1ControllerRevision> historyList = controlledHistory(api, daemonSet, selector);
        parseHistory(
                historyList);
    }

    private void statefulSetViewHistory(V1StatefulSet statefulSet, AppsV1Api api)
            throws ApiException, KubectlException {
        LabelSelector selector = LabelSelector.parse(statefulSet.getSpec().getSelector());
        List<V1ControllerRevision> historyList = controlledHistory(api, statefulSet, selector);
        parseHistory(
                historyList);
    }

    private void parseHistory(List<V1ControllerRevision> historyList)
            throws ApiException, KubectlException {
        Map<Long, V1ControllerRevision> historyInfo = new HashMap<>();
        for (V1ControllerRevision history : historyList) {
            historyInfo.put(history.getRevision(), history);
        }

        if (revision > 0) {

            return;
        }

        List<Long> revisions = new ArrayList<>(historyInfo.keySet());
        revisions.sort(Long::compareTo);
        for (Long revision : revisions) {
            String changeCause = getChangeCause(historyInfo.get(revision).getMetadata());
            if (changeCause == null || changeCause.isEmpty()) {
                changeCause = "<none>";
            }
            histories.add(new History(revision, changeCause));
        }
    }

    // controlledHistories returns all ControllerRevisions in namespace that selected by selector
    // and
    // owned by accessor
    private List<V1ControllerRevision> controlledHistory(
            AppsV1Api api, KubernetesObject accessor, LabelSelector selector) throws ApiException {
        List<V1ControllerRevision> result = new ArrayList<>();
        V1ControllerRevisionList historyList =
                api.listNamespacedControllerRevision(namespace, null, null, null, null,selector.toString(), null, null, null, null, null, null);
        for (V1ControllerRevision history : historyList.getItems()) {
            if (isControlledBy(history, accessor)) {
                result.add(history);
            }
        }
        return result;
    }

    private boolean isControlledBy(KubernetesObject obj, KubernetesObject owner) {
        return obj.getMetadata().getOwnerReferences().stream()
                .filter(r -> r.getController() != null && r.getController())
                .findAny()
                .map(v1OwnerReference -> v1OwnerReference.getUid().equals(owner.getMetadata().getUid()))
                .orElse(false);
    }

    // getChangeCause returns the change-cause annotation of the input object
    private String getChangeCause(V1ObjectMeta meta) {
        if (meta.getAnnotations() == null) {
            return null;
        }
        return meta.getAnnotations().get(CHANGE_CAUSE_ANNOTATION);
    }

    public record History(long revision, String changeCause) {
    }
}