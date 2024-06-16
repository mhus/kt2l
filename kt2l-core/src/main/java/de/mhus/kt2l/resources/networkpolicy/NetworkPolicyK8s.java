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

package de.mhus.kt2l.resources.networkpolicy;

import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.CallBackAdapter;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1NetworkPolicy;
import io.kubernetes.client.openapi.models.V1NetworkPolicyList;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NetworkPolicyK8s implements HandlerK8s {

    @Autowired
    private SecurityService securityService;

    @Override
    public K8s getManagedResourceType() {
        return K8s.NETWORK_POLICY;
    }

    @Override
    public String getDescribe(ApiProvider apiProvider, KubernetesObject res) {
        var sb = new StringBuilder();
        K8sUtil.describeHeader(apiProvider, this, res, sb);
        if (res instanceof V1NetworkPolicy networkPolicy) {
            sb.append("Ingress: ").append(networkPolicy.getSpec().getIngress()).append("\n");
            sb.append("Egress: ").append(networkPolicy.getSpec().getEgress()).append("\n");
        }
        K8sUtil.describeFooter(apiProvider, this, res, sb);
        return sb.toString();

    }

    @Override
    public void replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1NetworkPolicy.class);
        var api = apiProvider.getNetworkingV1Api();
        api.replaceNamespacedNetworkPolicy(
                name, namespace, body, null, null, null, null
        );
    }

    @Override
    public V1Status delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        K8sUtil.checkDeleteAccess(securityService, K8s.NETWORK_POLICY);
        var api = apiProvider.getNetworkingV1Api();
        return api.deleteNamespacedNetworkPolicy(name, namespace, null, null, null, null, null, null);
    }

    @Override
    public Object create(ApiProvider apiProvider, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1NetworkPolicy.class);
        var api = apiProvider.getNetworkingV1Api();
        return api.createNamespacedNetworkPolicy(
                body.getMetadata().getNamespace() == null ? "default" : body.getMetadata().getNamespace(),
                body, null, null, null, null
        );
    }

    @Override
    public V1NetworkPolicyList createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getNetworkingV1Api().listNetworkPolicyForAllNamespaces(null, null, null, null, null, null, null, null, null, null, null);
    }

    @Override
    public V1NetworkPolicyList createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException {
        return apiProvider.getNetworkingV1Api().listNamespacedNetworkPolicy(namespace, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Override
    public Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getNetworkingV1Api().listNetworkPolicyForAllNamespacesCall(null, null, null, null, null, null, null, null, null, null, true, new CallBackAdapter(LOGGER));
    }

}