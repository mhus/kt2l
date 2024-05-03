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

package de.mhus.kt2l.resources.statefulset;

import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.HandlerK8s;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.Yaml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StatefulSetK8s implements HandlerK8s {

    @Autowired
    private SecurityService securityService;

    @Override
    public K8s.RESOURCE getManagedKind() {
        return K8s.RESOURCE.STATEFUL_SET;
    }

    @Override
    public void replace(CoreV1Api api, String name, String namespace, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1StatefulSet.class);
        AppsV1Api appsV1Api = new AppsV1Api(api.getApiClient());
        appsV1Api.replaceNamespacedStatefulSet(
                name, namespace,
                body
        ).execute();
    }

    @Override
    public V1Status delete(CoreV1Api api, String name, String namespace) throws ApiException {
        checkDeleteAccess(securityService, K8s.RESOURCE.STATEFUL_SET);
        AppsV1Api appsV1Api = new AppsV1Api(api.getApiClient());
        return appsV1Api.deleteNamespacedStatefulSet(name, namespace).execute();
    }

}
