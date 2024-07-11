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

package de.mhus.kt2l.resources.pod;

import de.mhus.commons.tools.MFile;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.generated.K8sV1Pod;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
@Component
public class PodK8s extends K8sV1Pod {

    @Override
    public String getDescribe(ApiProvider apiProvider, KubernetesObject object) {
        var sb = new StringBuilder();
        K8sUtil.describeHeader(apiProvider, this, object, sb);
        if (object instanceof V1Pod res) {

            sb.append("Phase:         ").append(res.getStatus().getPhase()).append("\n");
            sb.append("Node:          ").append(res.getSpec().getNodeName()).append("\n");
            sb.append("Priority:      ").append(res.getSpec().getPriority()).append("\n");
            sb.append("Restart:       ").append(res.getSpec().getRestartPolicy()).append("\n");
            sb.append("Termination:   ").append(res.getSpec().getTerminationGracePeriodSeconds()).append("\n");
            sb.append("IP:            ").append(res.getStatus().getPodIP()).append("\n");
            sb.append("Host IP:       ").append(res.getStatus().getHostIP()).append("\n");
            sb.append("IPs:           ").append(res.getStatus().getPodIPs()).append("\n");
            if (res.getSpec().getDnsPolicy() != null)
                sb.append("DNS Policy:    ").append(res.getSpec().getDnsPolicy()).append("\n");
            if (res.getSpec().getHostNetwork() != null)
                sb.append("Host Network:  ").append(res.getSpec().getHostNetwork()).append("\n");
            if (res.getSpec().getHostPID() != null)
                sb.append("Host PID:      ").append(res.getSpec().getHostPID()).append("\n");
            if (res.getSpec().getHostIPC() != null)
                sb.append("Host IPC:      ").append(res.getSpec().getHostIPC()).append("\n");
            if (res.getSpec().getServiceAccountName() != null)
                sb.append("Service Acc:   ").append(res.getSpec().getServiceAccountName()).append("\n");
            if (res.getSpec().getAutomountServiceAccountToken() != null)
                sb.append("Automount:     ").append(res.getSpec().getAutomountServiceAccountToken()).append("\n");
            if (res.getSpec().getImagePullSecrets() != null)
                sb.append("Image Pull:    ").append(res.getSpec().getImagePullSecrets()).append("\n");
            if (res.getSpec().getSecurityContext() != null)
                sb.append("Security Cont: ").append(res.getSpec().getSecurityContext()).append("\n");
            if (res.getSpec().getSubdomain() != null)
                sb.append("Subdomain:     ").append(res.getSpec().getSubdomain()).append("\n");
            if (res.getSpec().getAffinity() != null)
                sb.append("Affinity:      ").append(res.getSpec().getAffinity()).append("\n");
            if (res.getSpec().getSchedulerName() != null)
                sb.append("Scheduler:     ").append(res.getSpec().getSchedulerName()).append("\n");
            if (res.getSpec().getTolerations() != null)
                sb.append("Tolerations:   ").append(res.getSpec().getTolerations()).append("\n");
            if (res.getSpec().getHostAliases() != null)
                sb.append("Host Aliases:  ").append(res.getSpec().getHostAliases()).append("\n");
            if (res.getSpec().getTopologySpreadConstraints() != null)
                sb.append("Topology:      ").append(res.getSpec().getTopologySpreadConstraints()).append("\n");
            if (res.getSpec().getOverhead() != null)
                sb.append("Overhead:      ").append(res.getSpec().getOverhead()).append("\n");
            sb.append("Init Cont:     ").append(res.getSpec().getInitContainers()).append("\n");
            sb.append("Cont:          ").append(res.getSpec().getContainers()).append("\n");
            sb.append("Ephemeral:     ").append(res.getSpec().getEphemeralContainers()).append("\n");
            sb.append("Volumes:       ").append(res.getSpec().getVolumes()).append("\n");
            sb.append("Node Selector: ").append(res.getSpec().getNodeSelector()).append("\n");

        }
        K8sUtil.describeFooter(apiProvider, this, object, sb);

        if (object instanceof V1Pod res) {
            sb.append("\nLogs:\n\n");
            PodLogs podLogs = new PodLogs(apiProvider.getClient());
            try (var logStream = podLogs.streamNamespacedPodLog(
                    res.getMetadata().getNamespace(),
                    res.getMetadata().getName(),
                    null,
                    null,
                    10,
                    true
            )) {
                Thread.startVirtualThread(() -> {
                    try {
                        MThread.sleep(500);
                        logStream.close();
                    } catch (Exception e) {
                    }
                });
                BufferedReader reader = new BufferedReader(new InputStreamReader(logStream));
                MFile.readLines(reader, (line) -> {
                    sb.append(line).append("\n");
                });
            } catch (Exception e) {
                if (e.getMessage() != null && !e.getMessage().equals("stream closed"))
                    LOGGER.warn("Error reading logs for {}", res.getMetadata().getName(), e);
            }
        }
        return sb.toString();
    }

    public void deleteContainer(ApiProvider apiProvider, V1Pod pod, String containerName) throws ApiException {
        if (pod.getSpec().getContainers() != null)
            for (var c : pod.getSpec().getContainers()) {
                if (c.getName().equals(containerName)) {
                    pod.getSpec().getContainers().remove(c);
                    break;
                }
            }
        if (pod.getSpec().getInitContainers() != null)
            for (var c : pod.getSpec().getInitContainers()) {
                if (c.getName().equals(containerName)) {
                    pod.getSpec().getInitContainers().remove(c);
                    break;
                }
            }
        if (pod.getSpec().getEphemeralContainers() != null)
            for (var c : pod.getSpec().getEphemeralContainers()) {
                if (c.getName().equals(containerName)) {
                    pod.getSpec().getEphemeralContainers().remove(c);
                    apiProvider.getCoreV1Api().replaceNamespacedPodEphemeralcontainers(
                            pod.getMetadata().getName(),
                            pod.getMetadata().getNamespace(),
                            pod,
                            null, null, null, null
                    );
                    return;
                }
            }

        replaceResource(apiProvider, pod.getMetadata().getName(), pod.getMetadata().getNamespace(), pod);

    }
}