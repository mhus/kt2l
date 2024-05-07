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

package de.mhus.kt2l.resources.job;

import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.HandlerK8s;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.Yaml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JobK8s implements HandlerK8s {

    @Autowired
    private SecurityService securityService;

    @Override
    public K8s.RESOURCE getManagedResource() {
        return K8s.RESOURCE.JOB;
    }

    @Override
    public void replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1Job.class);
        BatchV1Api batchV1Api = new BatchV1Api(apiProvider.getClient());
        batchV1Api.replaceNamespacedJob(
                name, namespace,
                body
        ).execute();
    }

    @Override
    public V1Status delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        checkDeleteAccess(securityService, K8s.RESOURCE.JOB);
        BatchV1Api batchV1Api = new BatchV1Api(apiProvider.getClient());
        return batchV1Api.deleteNamespacedJob(name, namespace).execute();
    }

    @Override
    public Object create(ApiProvider apiProvider, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1Job.class);
        BatchV1Api batchV1Api = new BatchV1Api(apiProvider.getClient());
        return batchV1Api.createNamespacedJob(
                body.getMetadata().getNamespace(),
                body
        ).execute();
    }

}
