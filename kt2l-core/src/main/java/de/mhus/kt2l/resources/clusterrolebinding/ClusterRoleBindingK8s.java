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

package de.mhus.kt2l.resources.clusterrolebinding;

import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.RbacAuthorizationV1Api;
import io.kubernetes.client.openapi.models.V1ClusterRoleBinding;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.Yaml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClusterRoleBindingK8s implements HandlerK8s {

    @Autowired
    private SecurityService securityService;

    @Override
    public K8s.RESOURCE getManagedResource() {
        return K8s.RESOURCE.CLUSTER_ROLE_BINDING;
    }

    @Override
    public void replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1ClusterRoleBinding.class);
        RbacAuthorizationV1Api authenticationV1Api = new RbacAuthorizationV1Api(apiProvider.getClient());
        authenticationV1Api.replaceClusterRoleBinding(
                name,
                body
        ).execute();
    }

    @Override
    public Object delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        // this is dangerous ... deny!
        checkDeleteAccess(securityService, K8s.RESOURCE.CLUSTER_ROLE_BINDING);
        RbacAuthorizationV1Api authenticationV1Api = new RbacAuthorizationV1Api(apiProvider.getClient());
        return authenticationV1Api.deleteClusterRoleBinding(name).execute();
    }

    @Override
    public Object create(ApiProvider apiProvider, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1ClusterRoleBinding.class);
        RbacAuthorizationV1Api authenticationV1Api = new RbacAuthorizationV1Api(apiProvider.getClient());
        return authenticationV1Api.createClusterRoleBinding(body).execute();
    }
}
