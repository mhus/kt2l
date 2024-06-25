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
import de.mhus.kt2l.generated.K8sV1Node;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
            sb.append("Conditions:    ").append(node.getStatus().getConditions()).append("\n");
            sb.append("Addresses:     ").append(node.getStatus().getAddresses()).append("\n");
            sb.append("Allocatable:   \n");
            node.getStatus().getAllocatable().forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v.getNumber()).append("\n"));
            sb.append("Capacity:      \n");
            node.getStatus().getCapacity().forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v.getNumber()).append("\n"));
            try {
                final var pods = apiProvider.getCoreV1Api().listPodForAllNamespaces(null, null, "spec.nodeName=" + node.getMetadata().getName(), null, null, null, null, null, null, null, null);
                if (pods.getItems().size() > 0) {
                    sb.append("\nPods:\n");
                    var table = new ConsoleTable();
                    table.setHeaderValues("Name", "Namespace", "Status", "Age");
                    for (var pod : pods.getItems()) {
                        table.addRowValues(pod.getMetadata().getName(), pod.getMetadata().getNamespace(), pod.getStatus().getPhase(), K8sUtil.getAge(pod.getMetadata().getCreationTimestamp()));
                    }
                    sb.append(table).append("\n\n");
                }
            } catch (ApiException e) {
                LOGGER.error("Error loading pods for node {}", node, e);
            }
            sb.append("Images:        ").append(node.getStatus().getImages()).append("\n");

        }
        K8sUtil.describeFooter(apiProvider, this, res, sb);
        return sb.toString();
    }

}
