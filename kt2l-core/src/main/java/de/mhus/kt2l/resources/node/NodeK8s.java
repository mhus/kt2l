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
import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.CallBackAdapter;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.k8s.K8s;
import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NodeK8s implements HandlerK8s {

    @Autowired
    private SecurityService securityService;

    @Override
    public K8s getManagedResource() {
        return K8s.NODE;
    }

    @Override
    public String getPreview(ApiProvider apiProvider, KubernetesObject res) {
        var sb = new StringBuilder();
        K8sUtil.previewHeader(apiProvider, this, res, sb);

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
        K8sUtil.previewFooter(apiProvider, this, res, sb);
        return sb.toString();
    }

    @Override
    public void replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        // this is dangerous ... deny like delete!
        checkDeleteAccess(securityService, K8s.NODE);
        var body = Yaml.loadAs(yaml, V1Node.class);
        apiProvider.getCoreV1Api().replaceNode(
                name,
                body,
                null, null, null, null
        );
    }

    @Override
    public V1Status delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        // this is dangerous ... deny!
        checkDeleteAccess(securityService, K8s.NODE);
        return apiProvider.getCoreV1Api().deleteNode(name, null, null, null, null, null, null );
    }

    @Override
    public Object create(ApiProvider apiProvider, String yaml) throws ApiException {
        // this is dangerous ... deny! - or stupid?
        checkDeleteAccess(securityService, K8s.NODE);
        var body = Yaml.loadAs(yaml, V1Node.class);
        return apiProvider.getCoreV1Api().createNode(body,null, null, null, null);
    }

    @Override
    public <L extends KubernetesListObject> L createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException {
        throw new NotImplementedException();
    }

    @Override
    public Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getCoreV1Api().listNodeCall(null, null, null, null, null, null, null, null, null, null, true, new CallBackAdapter(LOGGER));
    }

    @Override
    public V1NodeList createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getCoreV1Api().listNode(null, null, null, null, null, null, null, null, null, null, null);
    }
}
