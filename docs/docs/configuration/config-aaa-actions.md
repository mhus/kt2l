---
sidebar_position: 20
title: Authorization Actions
---

# Authorization Configuration Actions

This document lists all known actions and their default roles.

```yaml
cluster_action:
  de.mhus.kt2l.core.ResourcesClusterAction: READ
  de.mhus.kt2l.events.EventClusterAction: READ
  de.mhus.kt2l.flux.FluxLogsClusterAction: READ
  de.mhus.kt2l.helm.HelmClusterAction: READ
  de.mhus.kt2l.portforward.PortForwardClusterAction: WRITE, LOCAL
  de.mhus.kt2l.resources.common.ClusterCreateAction: WRITE
  de.mhus.kt2l.system.ClusterVersionAction: READ
  de.mhus.kt2l.vis.VisClusterAction: READ

core_action:
  de.mhus.kt2l.core.LocalBashCoreAction: ADMIN
  de.mhus.kt2l.storage.StoragePanelCoreAction: READ
  de.mhus.kt2l.system.DevelopmentAction: ADMIN
  de.mhus.kt2l.system.SystemInfoAction: ADMIN

resource_action:
  de.mhus.kt2l.ai.AiAction: READ
  de.mhus.kt2l.events.EventResourceAction: READ
  de.mhus.kt2l.resources.clusterrole.ShowClusterRoleBindingsForClusterRoleAction: READ
  de.mhus.kt2l.resources.clusterrolebinding.ShowServiceAccountsForClusterRoleBindingAction: READ
  de.mhus.kt2l.resources.common.ActionDelete: WRITE
  de.mhus.kt2l.resources.common.AddAnnotationAction: WRITE
  de.mhus.kt2l.resources.common.AddLabelAction: WRITE
  de.mhus.kt2l.resources.common.DescribeAction: READ
  de.mhus.kt2l.resources.common.DummyAction: WRITE
  de.mhus.kt2l.resources.common.RemoveAnnotationAction: WRITE
  de.mhus.kt2l.resources.common.RemoveLabelAction: WRITE
  de.mhus.kt2l.resources.common.ResourceActionYamlEditor: READ
  de.mhus.kt2l.resources.common.ResourceCreateAction: WRITE
  de.mhus.kt2l.resources.common.ResourceEditFormAction: WRITE
  de.mhus.kt2l.resources.common.ResourcePatchAction: WRITE
  de.mhus.kt2l.resources.common.ShowOwnerOfResourceAction: READ
  de.mhus.kt2l.resources.configmap.EditConfigMapAction: WRITE
  de.mhus.kt2l.resources.configmap.ShowPodsUsingConfigMapAction: READ
  de.mhus.kt2l.resources.cronjob.CronJobSuspendAction: WRITE
  de.mhus.kt2l.resources.cronjob.ShowJobsOfCronJobAction: READ
  de.mhus.kt2l.resources.cronjob.ShowPodsOfCronJobAction: READ
  de.mhus.kt2l.resources.daemonset.ShowPodsOfDaemonSetAction: READ
  de.mhus.kt2l.resources.deployment.ScaleDeploymentAction: WRITE
  de.mhus.kt2l.resources.deployment.ShowPodsOfDeploymentAction: READ
  de.mhus.kt2l.resources.deployment.ShowReplicaSetsOfDeploymentAction: READ
  de.mhus.kt2l.resources.job.ShowPodsOfJobAction: READ
  de.mhus.kt2l.resources.namespace.ShowPodsOfNamespaceAction: READ
  de.mhus.kt2l.resources.node.AddTaintAction: ADMIN
  de.mhus.kt2l.resources.node.CordonNodeAction: ADMIN
  de.mhus.kt2l.resources.node.DrainNodeAction: ADMIN
  de.mhus.kt2l.resources.node.RemoveTaintAction: ADMIN
  de.mhus.kt2l.resources.node.ShowPodsOfNodeAction: READ
  de.mhus.kt2l.resources.node.UnCordonNodeAction: ADMIN
  de.mhus.kt2l.resources.persistentvolume.ShowClaimForVolumeAction: READ
  de.mhus.kt2l.resources.persistentvolume.ShowPodsWithVolumeAction: READ
  de.mhus.kt2l.resources.persistentvolumeclaim.ShowVolumesForClaimAction: READ
  de.mhus.kt2l.resources.pod.ContainerResizeLimitsAction: WRITE
  de.mhus.kt2l.resources.pod.CreateDebugContainerAction: ADMIN
  de.mhus.kt2l.resources.pod.HighlightAlertsToggelAction: READ
  de.mhus.kt2l.resources.pod.OpenPodPortForwardAction: WRITE, LOCAL
  de.mhus.kt2l.resources.pod.PodAttachAction: WRITE
  de.mhus.kt2l.resources.pod.PodExecAction: WRITE
  de.mhus.kt2l.resources.pod.PodLogsAction: READ
  de.mhus.kt2l.resources.pod.PodShellAction: WRITE
  de.mhus.kt2l.resources.pod.ShowNodeOfPodAction: READ
  de.mhus.kt2l.resources.pod.TerminalAction: WRITE, LOCAL
  de.mhus.kt2l.resources.replicaset.ScaleReplicaSetAction: WRITE
  de.mhus.kt2l.resources.replicaset.ShowPodsOfReplicaSetAction: READ
  de.mhus.kt2l.resources.role.ShowRoleBindingsForRoleAction: READ
  de.mhus.kt2l.resources.rolebinding.ShowServiceAccountsForRoleBindingAction: READ
  de.mhus.kt2l.resources.secret.EditSecretAction: WRITE
  de.mhus.kt2l.resources.secret.ShowPodsUsingSecretAction: READ
  de.mhus.kt2l.resources.service.OpenSvcPortForwardAction: WRITE, LOCAL
  de.mhus.kt2l.resources.service.ShowPodsOfServiceAction: READ
  de.mhus.kt2l.resources.serviceaccount.ShowPodsForServiceAccountAction: READ
  de.mhus.kt2l.resources.statefulset.ScaleStatefulSetAction: WRITE
  de.mhus.kt2l.resources.statefulset.ShowPodsOfStatefulSetAction: READ

resource_grid:
  de.mhus.kt2l.resources.clusterrole.ClusterRoleGridFactory: READ
  de.mhus.kt2l.resources.clusterrolebinding.ClusterRoleBindingGridFactory: READ
  de.mhus.kt2l.resources.configmap.ConfigMapGridFactory: READ
  de.mhus.kt2l.resources.cronjob.CronJobGridFactory: READ
  de.mhus.kt2l.resources.daemonset.DaemonSetGridFactory: READ
  de.mhus.kt2l.resources.deployment.DeploymentGridFactory: READ
  de.mhus.kt2l.resources.endpoint.EndpointGridFactory: READ
  de.mhus.kt2l.resources.generic.GenericGridFactory: READ
  de.mhus.kt2l.resources.hpa.HorizontalPodAutoscalerGridFactory: READ
  de.mhus.kt2l.resources.ingress.IngressGridFactory: READ
  de.mhus.kt2l.resources.job.JobGridFactory: READ
  de.mhus.kt2l.resources.namespace.NamespaceGridFactory: READ
  de.mhus.kt2l.resources.networkpolicy.NetworkPolicyGridFactory: READ
  de.mhus.kt2l.resources.node.NodeGridFactory: READ
  de.mhus.kt2l.resources.persistentvolume.PersistentVolumeGridFactory: READ
  de.mhus.kt2l.resources.persistentvolumeclaim.PersistentVolumeClaimGridFactory: READ
  de.mhus.kt2l.resources.pod.PodGridFactory: READ
  de.mhus.kt2l.resources.replicaset.ReplicaSetGridFactory: READ
  de.mhus.kt2l.resources.role.RoleGridFactory: READ
  de.mhus.kt2l.resources.rolebinding.RoleBin