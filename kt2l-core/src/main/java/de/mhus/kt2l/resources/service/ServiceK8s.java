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

package de.mhus.kt2l.resources.service;

import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.generated.K8sV1Service;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.CallBackAdapter;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.util.PatchUtils;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ServiceK8s extends K8sV1Service {

    @Override
    public String getDescribe(ApiProvider apiProvider, KubernetesObject res) {
        var sb = new StringBuilder();
        K8sUtil.describeHeader(apiProvider, this, res, sb);
        if (res instanceof V1Service service) {
            sb.append("Type:          ").append(service.getSpec().getType()).append("\n");
            sb.append("Cluster IP:    ").append(service.getSpec().getClusterIP()).append("\n");
            sb.append("External IP:   ").append(service.getSpec().getExternalIPs()).append("\n");
            if (service.getStatus().getLoadBalancer() != null) {
                sb.append("IPs :          ").append(service.getStatus().getLoadBalancer().getIngress()).append("\n");
                }
            sb.append("Ports:\n");
            for (var port : service.getSpec().getPorts()) {
                sb.append("  ").append(port.getName()).append(": ").append(port.getPort()).append(" -> ").append(port.getTargetPort()).append("\n");
            }
            if (service.getSpec().getSelector() != null) {
                sb.append("Selector:\n");
                for (var entry : service.getSpec().getSelector().entrySet()) {
                    sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }
            if (service.getSpec().getSessionAffinity() != null) {
                sb.append("Session Affinity: ").append(service.getSpec().getSessionAffinity()).append("\n");
            }
        }
        K8sUtil.describeFooter(apiProvider, this, res, sb);
        return sb.toString();
    }

}
