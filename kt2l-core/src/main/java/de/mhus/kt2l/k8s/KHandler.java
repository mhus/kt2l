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

package de.mhus.kt2l.k8s;

import de.mhus.kt2l.config.AaaConfiguration;
import de.mhus.kt2l.core.SecurityService;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;

public interface KHandler {

    K8s.RESOURCE getManagedKind();

    void replace(CoreV1Api api, String name, String namespace, String yaml) throws ApiException;

    void delete(CoreV1Api api, String name, String namespace) throws ApiException;

    default void checkDeleteAccess(SecurityService securityService, K8s.RESOURCE resource) throws ApiException {
        var defaultRole = securityService.getRolesForResource(AaaConfiguration.SCOPE_DEFAULT, AaaConfiguration.SCOPE_RESOURCE_DELETE);
        if (!securityService.hasRole(AaaConfiguration.SCOPE_RESOURCE_DELETE, resource.resourceType(), defaultRole))
            throw new ApiException(403, "Access denied for non admin users");
    }

}
