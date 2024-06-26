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

package de.mhus.kt2l.resources.hpa;

import de.mhus.kt2l.generated.K8sV1HorizontalPodAutoscaler;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1HorizontalPodAutoscaler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HorizontalPodAutoscalerK8s extends K8sV1HorizontalPodAutoscaler {

    @Override
    public String getDescribe(ApiProvider apiProvider, KubernetesObject res) {
        var sb = new StringBuilder();
        K8sUtil.describeHeader(apiProvider, this, res, sb);
        if (res instanceof V1HorizontalPodAutoscaler horizontalPodAutoscaler) {
            sb.append("Scale Target Ref: ").append(horizontalPodAutoscaler.getSpec().getScaleTargetRef().getName()).append("\n");
            sb.append("Min Replicas: ").append(horizontalPodAutoscaler.getSpec().getMinReplicas()).append("\n");
            sb.append("Max Replicas: ").append(horizontalPodAutoscaler.getSpec().getMaxReplicas()).append("\n");
            sb.append("Desired Replicas: ").append(horizontalPodAutoscaler.getStatus().getDesiredReplicas()).append("\n");
            sb.append("Current Replicas: ").append(horizontalPodAutoscaler.getStatus().getCurrentReplicas()).append("\n");
            sb.append("Last Scale Time: ").append(horizontalPodAutoscaler.getStatus().getLastScaleTime()).append("\n");
            sb.append("Target CPU Utilization: ").append(horizontalPodAutoscaler.getSpec().getTargetCPUUtilizationPercentage()).append("\n");
            sb.append("Current CPU Utilization: ").append(horizontalPodAutoscaler.getStatus().getCurrentCPUUtilizationPercentage()).append("\n");
        }
        K8sUtil.describeFooter(apiProvider, this, res, sb);
        return sb.toString();

    }

}
