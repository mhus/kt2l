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

package de.mhus.kt2l.resources.ingress;

import de.mhus.kt2l.generated.K8sV1Ingress;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1Ingress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IngressK8s extends K8sV1Ingress {

    @Override
    public String getDescribe(ApiProvider apiProvider, KubernetesObject res) {
        var sb = new StringBuilder();
        K8sUtil.describeHeader(apiProvider, this, res, sb);
        if (res instanceof V1Ingress ingress) {
            sb.append("Address: ").append(ingress.getStatus().getLoadBalancer().getIngress()).append("\n");
            sb.append("Ingress Class: ").append(ingress.getSpec().getIngressClassName()).append("\n");
            sb.append("TLS: ").append(ingress.getSpec().getTls()).append("\n");
            if (ingress.getSpec().getRules() != null) {
                sb.append("Rules: ").append("\n");
                ingress.getSpec().getRules().forEach(rule -> {
                    sb.append("  Host: ").append(rule.getHost()).append("\n");
                    sb.append("  Path: ").append(rule.getHttp().getPaths()).append("\n");
                });
            }
        }
        K8sUtil.describeFooter(apiProvider, this, res, sb);
        return sb.toString();
    }

}
