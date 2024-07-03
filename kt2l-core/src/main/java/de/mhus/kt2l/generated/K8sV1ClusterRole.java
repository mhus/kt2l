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
package de.mhus.kt2l.generated;

import de.mhus.kt2l.aaa.SecurityService;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.CallBackAdapter;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1ClusterRole;
import io.kubernetes.client.openapi.models.V1ClusterRoleList;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class K8sV1ClusterRole implements HandlerK8s {

    @Autowired
    private SecurityService securityService;

    @Override
    public V1APIResource getManagedType() {
        return K8s.CLUSTER_ROLE;
    }

    @Override
    public Object replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1ClusterRole.class);
        return apiProvider.getRbacAuthorizationV1Api().replaceClusterRole(
            name,
            body,
            null, null, null, null
        );
    }

    @Override
    public Object delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        K8sUtil.checkDeleteAccess(securityService, K8s.CLUSTER_ROLE);
        return apiProvider.getRbacAuthorizationV1Api().deleteClusterRole(
            name,
            null, null, null, null, null, null
        );
    }

    @Override
    public Object create(ApiProvider apiProvider, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1ClusterRole.class);
        return apiProvider.getRbacAuthorizationV1Api().createClusterRole(
            body,
            null, null, null, null
        );
    }

    @Override
    public V1ClusterRoleList createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getRbacAuthorizationV1Api().listClusterRole(
            null, null, null, null, null, null, null, null, null, null, null
        );
    }

    @Override
    public V1ClusterRoleList createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException {
      throw new NotImplementedException();
    }

    @Override
    public Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getRbacAuthorizationV1Api().listClusterRoleCall(
            null, null, null, null, null, null, null, null, null, null, true,
            new CallBackAdapter<V1ClusterRole>(LOGGER)
        );
    }

    @Override
    public Object patch(ApiProvider apiProvider, String namespace, String name, String patchString) throws ApiException {
        var patch = new V1Patch(patchString);
        return apiProvider.getRbacAuthorizationV1Api().patchClusterRoleCall(
            name,
            patch,
            null, null, null, null, null, null
        );
    }

}
