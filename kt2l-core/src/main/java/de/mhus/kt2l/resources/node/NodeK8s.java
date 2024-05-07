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

package de.mhus.kt2l.resources.node;

import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.HandlerK8s;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.Yaml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NodeK8s implements HandlerK8s {

    @Autowired
    private SecurityService securityService;

    @Override
    public K8s.RESOURCE getManagedResource() {
        return K8s.RESOURCE.NODE;
    }

    @Override
    public void replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        // this is dangerous ... deny like delete!
        checkDeleteAccess(securityService, K8s.RESOURCE.NODE);
        var body = Yaml.loadAs(yaml, V1Node.class);
        apiProvider.getCoreV1Api().replaceNode(
                name,
                body
        ).execute();
    }

    @Override
    public V1Status delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        // this is dangerous ... deny!
        checkDeleteAccess(securityService, K8s.RESOURCE.NODE);
        return apiProvider.getCoreV1Api().deleteNode(name).execute();
    }

    @Override
    public Object create(ApiProvider apiProvider, String yaml) throws ApiException {
        // this is dangerous ... deny! - or stupid?
        checkDeleteAccess(securityService, K8s.RESOURCE.NODE);
        var body = Yaml.loadAs(yaml, V1Node.class);
        return apiProvider.getCoreV1Api().createNode(body).execute();
    }
}
