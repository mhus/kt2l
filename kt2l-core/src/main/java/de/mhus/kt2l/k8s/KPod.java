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

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreApi;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Yaml;
import org.springframework.stereotype.Component;

@Component
public class KPod implements KHandler {

    @Override
    public String getManagedKind() {
        return K8sUtil.KIND_POD;
    }

    @Override
    public void replace(CoreV1Api api, String name, String namespace, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1Pod.class);
        api.replaceNamespacedPod(
                name,
                namespace,
                body, null, null, null, null
        );
    }

    @Override
    public void delete(CoreV1Api api, String name, String namespace) throws ApiException {
        api.deleteNamespacedPod(name, namespace, null, null, null, null, null, null);
    }

}
