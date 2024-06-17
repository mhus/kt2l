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
package de.mhus.kt2l.resources.generic;

import de.mhus.commons.errors.NotSupportedException;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.Yaml;
import okhttp3.Call;

public class GenericK8s implements HandlerK8s {

    private final V1APIResource resourceType;

    public GenericK8s(K8s resourceType) {
        this(K8s.toResource(resourceType));
    }

    public GenericK8s(V1APIResource resourceType) {
        this.resourceType = resourceType;
    }
    @Override
    public K8s getManagedResourceType() {
        return K8s.GENERIC;
    }

    @Override
    public String getDescribe(ApiProvider apiProvider, KubernetesObject res) {
        var sb = new StringBuilder();
        K8sUtil.describeHeader(apiProvider, this, res, sb);
        sb.append(Yaml.dump(res));
        K8sUtil.describeFooter(apiProvider, this, res, sb);
        return sb.toString();
    }

    @Override
    public void replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        throw new NotSupportedException("Not supported for generic resources");
//        var y = MYaml.loadFromString(yaml);
////        var kind = y.asMap().getString("Kind");
////        var resource = Arrays.stream(K8s.RESOURCE.values()).filter(r -> r.kind().equalsIgnoreCase(kind)).findFirst().orElse(null);
////        if (resource == null) throw new ApiException("Kind not found: " + kind);
////        var v1Resource = K8s.toResource(resource);
//        var genericApi = new GenericObjectsApi(apiProvider.getClient(), resourceType );
//        genericApi.replace(name, namespace, yaml);
    }

    @Override
    public V1Status delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        throw new NotSupportedException("Not supported for generic resources");
//        var genericApi = new GenericObjectsApi(apiProvider.getClient(), resourceType );
//        genericApi.delete(name, namespace);
//        return new V1Status(); //XXX
    }

    @Override
    public Object create(ApiProvider apiProvider, String yaml) throws ApiException {
        throw new NotSupportedException("Not supported for generic resources");
//        var genericApi = new GenericObjectsApi(apiProvider.getClient(), resourceType );
//        return genericApi.create(yaml);
    }

    @Override
    public GenericObjectList createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException {
        final var genericApi = new GenericObjectsApi(apiProvider.getClient(), resourceType);
        return genericApi.listNamespacedCustomObject(null);
    }

    @Override
    public Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException {
        throw new NotSupportedException("Not supported for generic resources");
    }

    @Override
    public GenericObjectList createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException {
        final var genericApi = new GenericObjectsApi(apiProvider.getClient(), resourceType);
        return genericApi.listNamespacedCustomObject(namespace);
    }

    @Override
    public Object patch(ApiProvider apiProvider, String namespace, String name, String patchString) throws ApiException {
        throw new NotSupportedException("Not supported for generic resources");
    }

}
