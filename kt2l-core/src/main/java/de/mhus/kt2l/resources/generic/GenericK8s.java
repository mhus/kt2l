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

import com.google.gson.JsonObject;
import de.mhus.commons.errors.NotSupportedException;
import de.mhus.commons.tools.MJson;
import de.mhus.commons.yaml.MYaml;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.util.Yaml;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesListObject;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;

@Slf4j
public class GenericK8s implements HandlerK8s {

    private final V1APIResource type;

    public GenericK8s(V1APIResource type) {
        this.type = type;
    }

    @Override
    public V1APIResource getManagedType() {
        return type;
    }

    @Override
    public String getDescribe(ApiProvider apiProvider, KubernetesObject res) {
        var sb = new StringBuilder();
        K8sUtil.describeHeader(apiProvider, this, res, sb);
        try {
            if (res instanceof DynamicKubernetesObject dynamicKubernetesObject) {
                sb.append(MYaml.toYaml(MJson.load(dynamicKubernetesObject.getRaw().toString()) ));
            } else {
                sb.append(Yaml.dump(res));
            }
        } catch (Exception e) {
            LOGGER.debug("Error dumping object", e);
        }
        K8sUtil.describeFooter(apiProvider, this, res, sb);
        return sb.toString();
    }

    @Override
    public Object replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        final var genericApi = new DynamicKubernetesApi(type.getGroup(), type.getVersion(), type.getName(), apiProvider.getClient());
        JsonObject jsonObject = Yaml.loadAs(yaml, JsonObject.class);
        DynamicKubernetesObject object = new DynamicKubernetesObject(jsonObject);
        return genericApi.update(object);
    }

    @Override
    public Object delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        final var genericApi = new DynamicKubernetesApi(type.getGroup(), type.getVersion(), type.getName(), apiProvider.getClient());
        if (type.getNamespaced())
            return genericApi.delete(namespace, name);
        else
            return genericApi.delete(name);
    }

    @Override
    public Object create(ApiProvider apiProvider, String yaml) throws ApiException {
        final var genericApi = new DynamicKubernetesApi(type.getGroup(), type.getVersion(), type.getName(), apiProvider.getClient());
        JsonObject jsonObject = Yaml.loadAs(yaml, JsonObject.class);
        DynamicKubernetesObject object = new DynamicKubernetesObject(jsonObject);
        return genericApi.create(object);
    }

    @Override
    public Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException {
        throw new NotSupportedException("Not supported for generic resources");
    }

    @Override
    public DynamicKubernetesListObject createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException {
        final var genericApi = new DynamicKubernetesApi(type.getGroup(), type.getVersion(), type.getName(), apiProvider.getClient());
        return genericApi.list(namespace).getObject();
    }

    @Override
    public DynamicKubernetesListObject createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException {
        final var genericApi = new DynamicKubernetesApi(type.getGroup(), type.getVersion(), type.getName(), apiProvider.getClient());
        return genericApi.list().getObject();
    }

    @Override
    public Object patch(ApiProvider apiProvider, String namespace, String name, String patchString) throws ApiException {
        final var genericApi = new DynamicKubernetesApi(type.getGroup(), type.getVersion(), type.getName(), apiProvider.getClient());

        var patch = new V1Patch(patchString);
        var patchType = "application/merge-patch+json"; // ???
        if (type.getNamespaced())
            return genericApi.patch(namespace, name, patchType, patch);
        else
            return genericApi.patch(name, patchType, patch);
    }

}
