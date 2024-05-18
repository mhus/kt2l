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
import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.CallBackAdapter;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.k8s.K8s;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
@Component
public class PodK8s implements HandlerK8s {

    @Autowired
    private SecurityService securityService;

    @Override
    public K8s getManagedResource() {
        return K8s.POD;
    }

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
            if (res.getSpec().getServiceAccountName()!=null)
                sb.append("Service Acc:   ").append(res.getSpec().getServiceAccountName()).append("\n");
            if (res.getSpec().getAutomountServiceAccountToken()!=null)
                sb.append("Automount:     ").append(res.getSpec().getAutomountServiceAccountToken()).append("\n");
            if (res.getSpec().getImagePullSecrets()!=null)
                sb.append("Image Pull:    ").append(res.getSpec().getImagePullSecrets()).append("\n");
            if (res.getSpec().getSecurityContext()!=null)
                sb.append("Security Cont: ").append(res.getSpec().getSecurityContext()).append("\n");
            if (res.getSpec().getSubdomain()!=null)
                sb.append("Subdomain:     ").append(res.getSpec().getSubdomain()).append("\n");
            if (res.getSpec().getAffinity()!=null)
                sb.append("Affinity:      ").append(res.getSpec().getAffinity()).append("\n");
            if (res.getSpec().getSchedulerName()!=null)
                sb.append("Scheduler:     ").append(res.getSpec().getSchedulerName()).append("\n");
            if (res.getSpec().getTolerations()!=null)
                sb.append("Tolerations:   ").append(res.getSpec().getTolerations()).append("\n");
            if (res.getSpec().getHostAliases()!=null)
                sb.append("Host Aliases:  ").append(res.getSpec().getHostAliases()).append("\n");
            if (res.getSpec().getTopologySpreadConstraints()!=null)
                sb.append("Topology:      ").append(res.getSpec().getTopologySpreadConstraints()).append("\n");
            if (res.getSpec().getOverhead()!=null)
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
                if (e.getMessage()!= null && !e.getMessage().equals("stream closed"))
                    LOGGER.warn("Error reading logs for {}", res.getMetadata().getName(), e);
            }
        }
        return sb.toString();
    }

    @Override
    public void replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1Pod.class);
        apiProvider.getCoreV1Api().replaceNamespacedPod(
                name,
                namespace,
                body,
                null, null, null, null
        );
    }

    @Override
    public KubernetesObject delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        K8sUtil.checkDeleteAccess(securityService, K8s.POD);
        return apiProvider.getCoreV1Api().deleteNamespacedPod(name, namespace, null, null, null, null, null, null);
    }

    @Override
    public KubernetesObject create(ApiProvider apiProvider, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1Pod.class);
        if (body.getSpec().getOverhead() != null && body.getSpec().getOverhead().size() == 0) {
            body.getSpec().setOverhead(null);
        }
        return apiProvider.getCoreV1Api().createNamespacedPod(body.getMetadata().getNamespace(), body, null, null, null, null);
    }

    @Override
    public V1PodList createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getCoreV1Api().listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null, null);
    }

    @Override
    public V1PodList createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException {
        return apiProvider.getCoreV1Api().listNamespacedPod(namespace,null, null, null, null, null, null, null, null, null, null, null);
    }

    @Override
    public Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getCoreV1Api().listPodForAllNamespacesCall(null, null, null, null, null, null, null, null, null, null, true, new CallBackAdapter<V1Pod>(LOGGER));
    }

}
