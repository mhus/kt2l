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

package de.mhus.kt2l.resources.namespace;

import de.mhus.kt2l.generated.K8sV1Namespace;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Namespace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NamespaceK8s extends K8sV1Namespace {

    @Override
    public String getDescribe(ApiProvider apiProvider, KubernetesObject res) {
        var sb = new StringBuilder();
        K8sUtil.describeHeader(apiProvider, this, res, sb);

        if (res instanceof V1Namespace namespace) {
            sb.append("Phase:         ").append(namespace.getStatus().getPhase()).append("\n\n");
            try {
                final var quotas = apiProvider.getCoreV1Api().listNamespacedResourceQuota(namespace.getMetadata().getName()).execute();
                quotas.getItems().forEach(q -> {
                    sb.append("ResourceQuota: ").append(q.getMetadata().getName()).append("\n");
                    sb.append("  Spec:        ").append(q.getSpec()).append("\n");
                    sb.append("  Status:      ").append(q.getStatus()).append("\n");
                });
                if (quotas.getItems().size() == 0)
                    sb.append("ResourceQuota: none\n");
            } catch (ApiException e) {
                LOGGER.error("Error loading ResourceQuota for {}", res, e);
            }
            sb.append("\n");
            try {
                final var limits = apiProvider.getCoreV1Api().listNamespacedLimitRange(namespace.getMetadata().getName()).execute();
                limits.getItems().forEach(q -> {
                    sb.append("LimitRange:    ").append(q.getMetadata().getName()).append("\n");
                    sb.append("  Spec:        ").append(q.getSpec()).append("\n");
                });
                if (limits.getItems().size() == 0)
                    sb.append("LimitRange: none\n");
            } catch (ApiException e) {
                LOGGER.error("Error loading LimitRange for {}", res, e);
            }
        }


        K8sUtil.describeFooter(apiProvider, this, res, sb);
        return sb.toString();
    }

}
