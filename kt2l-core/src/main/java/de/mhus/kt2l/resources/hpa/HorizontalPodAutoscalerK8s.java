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

import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.CallBackAdapter;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1HorizontalPodAutoscaler;
import io.kubernetes.client.openapi.models.V1HorizontalPodAutoscalerList;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.PatchUtils;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HorizontalPodAutoscalerK8s implements HandlerK8s {

    @Autowired
    private SecurityService securityService;

    @Override
    public K8s getManagedResourceType() {
        return K8s.HPA;
    }

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

    @Override
    public void replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1HorizontalPodAutoscaler.class);
        var api = apiProvider.getAutoscalingV1Api();
        api.replaceNamespacedHorizontalPodAutoscaler(
                name, namespace, body, null, null, null, null
        );
    }

    @Override
    public V1Status delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        K8sUtil.checkDeleteAccess(securityService, K8s.HPA);
        var api = apiProvider.getAutoscalingV1Api();
        return api.deleteNamespacedHorizontalPodAutoscaler(name, namespace, null, null, null, null, null, null);
    }

    @Override
    public Object create(ApiProvider apiProvider, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1HorizontalPodAutoscaler.class);
        var api = apiProvider.getAutoscalingV1Api();
        return api.createNamespacedHorizontalPodAutoscaler(
                body.getMetadata().getNamespace() == null ? "default" : body.getMetadata().getNamespace(),
                body, null, null, null, null
        );
    }

    @Override
    public V1HorizontalPodAutoscalerList createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getAutoscalingV1Api().listHorizontalPodAutoscalerForAllNamespaces(null, null, null, null, null, null, null, null, null, null, null);
    }

    @Override
    public V1HorizontalPodAutoscalerList createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException {
        return apiProvider.getAutoscalingV1Api().listNamespacedHorizontalPodAutoscaler(namespace, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Override
    public Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getAutoscalingV1Api().listHorizontalPodAutoscalerForAllNamespacesCall(null, null, null, null, null, null, null, null, null, null, true, new CallBackAdapter(LOGGER));
    }

    @Override
    public Object patch(ApiProvider apiProvider, String namespace, String name, String patchString) throws ApiException {
        V1Patch patch = new V1Patch(patchString);
        return PatchUtils.patch(
                V1HorizontalPodAutoscaler.class,
                () -> apiProvider.getAutoscalingV1Api().patchNamespacedHorizontalPodAutoscalerCall(
                        name,
                        namespace,
                        patch,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                V1Patch.PATCH_FORMAT_JSON_PATCH,
                apiProvider.getClient()
        );
    }

}
