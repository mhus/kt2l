/**
 * This file is part of kt2l-core.
 *
 * kt2l-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * kt2l-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with kt2l-core.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l.k8s;

import de.mhus.kt2l.config.AaaConfiguration;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.SecurityService;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.util.Yaml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KNode implements KHandler {

    @Autowired
    private SecurityService securityService;

    @Override
    public String getManagedKind() {
        return K8sUtil.KIND_NODE;
    }

    @Override
    public void replace(CoreV1Api api, String name, String namespace, String yaml) throws ApiException {
        // this is dangerous ... deny!
        if (!securityService.hasRole(AaaConfiguration.SCOPE_RESOURCE, K8sUtil.KIND_NODE, UsersConfiguration.ROLE.ADMIN.name()))
            throw new ApiException(403, "Access denied for non admin users");
        var body = Yaml.loadAs(yaml, V1Node.class);
        api.replaceNode(
                name,
                body, null, null, null, null
        );
    }

    @Override
    public void delete(CoreV1Api api, String name, String namespace) throws ApiException {
        // this is dangerous ... deny!
        if (!securityService.hasRole(AaaConfiguration.SCOPE_RESOURCE, K8sUtil.KIND_NODE, UsersConfiguration.ROLE.ADMIN.name()))
            throw new ApiException(403, "Access denied for non admin users");
        api.deleteNode(name, null, null, null, null, null, null);
    }
}
