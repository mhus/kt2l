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

package de.mhus.kt2l.resources.node;

import de.mhus.commons.console.ConsoleTable;
import de.mhus.commons.tools.MCast;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.generated.K8sV1Node;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.Metrics;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.PolicyV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.TreeMap;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
@Component
public class NodeK8s extends K8sV1Node {

    @Override
    public String getDescribe(ApiProvider apiProvider, KubernetesObject res) {
        var sb = new StringBuilder();
        K8sUtil.describeHeader(apiProvider, this, res, sb);

        if (res instanceof V1Node node) {
            sb.append("Taints:        ").append(node.getSpec().getTaints()).append("\n");
            sb.append("Unschedulable: ").append(node.getSpec().getUnschedulable()).append("\n");
            sb.append("Pod CIDR:      ").append(node.getSpec().getPodCIDR()).append("\n");
            sb.append("Provider ID:   ").append(node.getSpec().getProviderID()).append("\n");
            sb.append("External ID:   ").append(node.getSpec().getExternalID()).append("\n");
            sb.append("Internal IP:   ").append(node.getStatus().getAddresses()).append("\n");

            double cpuUsage = -1;
            long memoryUsage = -1;
            try {
                Metrics metrics = new Metrics(apiProvider.getClient());
                var list = metrics.getNodeMetrics().getItems();
                var metric = list.stream().filter(m -> m.getMetadata().getName().equals(node.getMetadata().getName())).findFirst().orElse(null);
                if (metric != null) {
                    sb.append("Metrics:\n");
                    sb.append("  CPU:         ").append(metric.getUsage().get("cpu")).append("\n");
                    sb.append("  Memory:      ").append(metric.getUsage().get("memory")).append("\n");
                    cpuUsage = metric.getUsage().get("cpu").getNumber().doubleValue();
                    memoryUsage = metric.getUsage().get("memory").getNumber().longValue();
                }
            } catch (Exception e) {
                sb.append("Metrics:       ").append(e.getMessage()).append("\n");
            }

            sb.append("Conditions:    \n");
            {
                var table = new ConsoleTable();
                table.setHeaderValues("Type", "Status", "Reason", "Last Transition", "Last Heartbeat", "Message");
                for (var condition : node.getStatus().getConditions()) {
                    table.addRowValues(condition.getType(), condition.getStatus(), condition.getReason(), K8sUtil.getAge(condition.getLastTransitionTime()), K8sUtil.getAge(condition.getLastHeartbeatTime()), condition.getMessage());
                }
                sb.append(table).append("\n");
            }
//            sb.append("Addresses:     ").append(node.getStatus().getAddresses()).append("\n");
            sb.append("Addresses:     \n");
            node.getStatus().getAddresses().forEach(address -> sb.append("  ").append(address.getType()).append(": ").append(address.getAddress()).append("\n"));

//            node.getStatus().getAllocatable().forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v.getNumber()).append("\n"));
//            node.getStatus().getCapacity().forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v.getNumber()).append("\n"));
            double sumCpuR = 0;
            long sumMemoryR = 0;
            double sumCpuL = 0;
            long sumMemoryL = 0;
            long sumEphemeralStorage = 0;
            long sumPods = 0;
            try {
                final var pods = apiProvider.getCoreV1Api().listPodForAllNamespaces(null, null, "spec.nodeName=" + node.getMetadata().getName(), null, null, null, null, null, null, null, null);
                sumPods = pods.getItems().size();
                if (pods.getItems().size() > 0) {
                    sb.append("\nPods: ").append(pods.getItems().size()).append("\n");
                    var table = new ConsoleTable();
                    table.setHeaderValues("Name", "Namespace", "Status", "Age", "CPU R", "CPU L", "Memory R", "Memory L", "Ephemeral Storage");
                    for (var pod : pods.getItems()) {
                        var cpuR = pod.getSpec().getContainers().stream().mapToDouble(c -> tryThis(() -> c.getResources().getRequests().get("cpu").getNumber().doubleValue()).orElse(0d) ).sum();
                        var memoryR = pod.getSpec().getContainers().stream().mapToDouble(c -> tryThis(() -> c.getResources().getRequests().get("memory").getNumber().longValue()).orElse(0L)).sum();
                        var cpuL = pod.getSpec().getContainers().stream().mapToDouble(c -> tryThis(() -> c.getResources().getLimits().get("cpu").getNumber().doubleValue()).orElse(0d) ).sum();
                        var memoryL = pod.getSpec().getContainers().stream().mapToDouble(c -> tryThis(() -> c.getResources().getLimits().get("memory").getNumber().longValue()).orElse(0L)).sum();
                        var ephemeralStorage = pod.getSpec().getContainers().stream().mapToDouble(c -> tryThis(() -> c.getResources().getLimits().get("ephemeral-storage").getNumber().longValue()).orElse(0L)).sum();
                        table.addRowValues(
                                pod.getMetadata().getName(),
                                pod.getMetadata().getNamespace(),
                                pod.getStatus().getPhase(),
                                K8sUtil.getAge(pod.getMetadata().getCreationTimestamp()),
                                cpuR, cpuL,
                                MString.toByteDisplayString((long)memoryR), MString.toByteDisplayString((long)memoryL),
                                MString.toByteDisplayString((long)ephemeralStorage)
                        );
                        sumCpuR += cpuR;
                        sumMemoryR += memoryR;
                        sumCpuL += cpuL;
                        sumMemoryL += memoryL;
                        sumEphemeralStorage += ephemeralStorage;
                    }
                    sb.append(table).append("\n\n");
                }
            } catch (ApiException e) {
                LOGGER.error("Error loading pods for node {}", node, e);
            }
            sb.append("Resources:   \n");
            {
                var data = new TreeMap<String, String[]>();
                node.getStatus().getAllocatable().forEach((k, v) -> data.put(k, new String[] {"",""}));
                node.getStatus().getCapacity().forEach((k, v) -> data.put(k, new String[] {"",""}));

                node.getStatus().getAllocatable().forEach((k, v) -> data.get(k)[0] = v.getNumber().toString());
                node.getStatus().getCapacity().forEach((k, v) -> data.get(k)[1] = v.getNumber().toString());

                var table = new ConsoleTable();
                table.setHeaderValues("Resource", "Capacity", "Allocatable", "Requested", "Req%", "Limit", "Limit%", "Usage", "Usage%");

                final var finalSumCpuR = sumCpuR;
                final var finalSumCpuL = sumCpuL;
                final var finalSumMemoryR = sumMemoryR;
                final var finalSumMemoryL = sumMemoryL;
                final var finalSumEphemeralStorage = sumEphemeralStorage;
                final var finalSumPods = sumPods;
                final var finalCpuUsage = cpuUsage;
                final var finalMemoryUsage = memoryUsage;

                data.forEach((k, v) -> {
                    if (k.equals("cpu")) {
                        var capacity = MCast.todouble(v[1], 0d);
                        var allocatable = MCast.todouble(v[0], 0d);
                        table.addRowValues(
                                k,
                                capacity,
                                allocatable,
                                finalSumCpuR,
                                Math.round(finalSumCpuR * 100 / allocatable),
                                finalSumCpuL,
                                Math.round(finalSumCpuL * 100 / allocatable),
                                finalCpuUsage == -1 ? "" : finalCpuUsage,
                                finalCpuUsage == -1 ? "" : Math.round(finalCpuUsage * 100 / allocatable)
                                );
                    } else
                    if (k.equals("memory")) {
                        var capacity = MCast.tolong(v[1], 0L);
                        var allocatable = MCast.tolong(v[0], 0L);
                        table.addRowValues(
                                k,
                                MString.toByteDisplayString(capacity),
                                MString.toByteDisplayString(allocatable),
                                MString.toByteDisplayString(finalSumMemoryR),
                                Math.round(finalSumMemoryR * 100 / allocatable),
                                MString.toByteDisplayString(finalSumMemoryL),
                                Math.round(finalSumMemoryL * 100 / allocatable),
                                finalMemoryUsage == -1 ? "" : MString.toByteDisplayString(finalMemoryUsage),
                                finalMemoryUsage == -1 ? "" : Math.round(finalMemoryUsage * 100 / allocatable)
                                );
                    } else
                        if (k.equals("ephemeral-storage")) {
                            var capacity = MCast.tolong(v[1], 0L);
                            var allocatable = MCast.tolong(v[0], 0L);
                            table.addRowValues(
                                    k,
                                    MString.toByteDisplayString(capacity),
                                    MString.toByteDisplayString(allocatable),
                                    MString.toByteDisplayString(finalSumEphemeralStorage),
                                    Math.round(finalSumEphemeralStorage * 100 / allocatable),
                                    "", "", "", ""
                            );
                    } else
                    if (k.equals("pods")) {
                        var capacity = MCast.tolong(v[1], 0L);
                        var allocatable = MCast.tolong(v[0], 0L);
                        table.addRowValues(
                                k,
                                capacity,
                                allocatable,
                                finalSumPods,
                                Math.round(finalSumPods * 100 / allocatable),
                                "", "", "", ""
                        );
                    } else
                        table.addRowValues(
                                k,
                                v[1],
                                v[0],
                                "", "", "", "", "", ""
                        );
                } );
                sb.append(table).append("\n");
            }
            sb.append("Images:\n");
                for (var image : node.getStatus().getImages()) {
                    image.getNames().forEach(n -> sb.append("  ").append(n).append("\n"));
                    sb.append("    ").append(MString.toByteDisplayString(image.getSizeBytes())).append("\n");
                }
        }
        K8sUtil.describeFooter(apiProvider, this, res, sb);
        return sb.toString();
    }

    @Override
    public Object delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        throw new ApiException("Delete is not possible for nodes");
    }

}
