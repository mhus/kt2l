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
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.util.PatchUtils;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Generated;

@Slf4j
@Generated("de.mhus.kt2l.k8s.GenerateK8sHandlers")
public abstract class K8sV1Node implements HandlerK8s {

    @Autowired
    protected SecurityService securityService;

    @Override
    public V1APIResource getManagedType() {
        return K8s.NODE;
    }

    @Override
    public KubernetesObject get(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        return apiProvider.getCoreV1Api().readNode(
            name
        ).execute();
    }

    @Override
    public Object replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        var res = Yaml.loadAs(yaml, V1Node.class);
        return replaceResource(apiProvider, name, namespace, res);
    }

    public Object replaceResource(ApiProvider apiProvider, String name, String namespace, V1Node resource) throws ApiException {
        return apiProvider.getCoreV1Api().replaceNode(
            name,
            resource
        ).execute();
    }

    @Override
    public Object delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        K8sUtil.checkDeleteAccess(securityService, K8s.NODE);
        return apiProvider.getCoreV1Api().deleteNode(
            name
        ).execute();
    }

    @Override
    public Object create(ApiProvider apiProvider, String yaml) throws ApiException {
        var body = Yaml.loadAs(yaml, V1Node.class);
        return apiProvider.getCoreV1Api().createNode(
            body
        ).execute();
    }

    @Override
    public V1NodeList createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getCoreV1Api().listNode().execute();
    }

    @Override
    public V1NodeList createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException {
      throw new NotImplementedException();
    }

    @Override
    public Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getCoreV1Api().listNode().watch(true).buildCall(
            new CallBackAdapter<V1Node>(LOGGER)
        );
    }

    @Override
    public Object patch(ApiProvider apiProvider, String namespace, String name, String patchString) throws ApiException {
        var patch = new V1Patch(patchString);
        return PatchUtils.patch(
            V1Node.class,
            () -> apiProvider.getCoreV1Api().patchNode(
                    name,
                    patch
            ).buildCall(new CallBackAdapter<V1Node>(LOGGER)),
            V1Patch.PATCH_FORMAT_JSON_PATCH,
            apiProvider.getClient()
        );
    }

}
