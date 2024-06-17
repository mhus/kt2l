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

package de.mhus.kt2l.resources.persistentvolumeclaim;

import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.CallBackAdapter;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimList;
import io.kubernetes.client.util.PatchUtils;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PersistentVolumeClaimK8s implements HandlerK8s {

    @Autowired
    private SecurityService securityService;

    @Override
    public K8s getManagedResourceType() {
        return K8s.PERSISTENT_VOLUME_CLAIM;
    }

    @Override
    public String getDescribe(ApiProvider apiProvider, KubernetesObject res) {
        var sb = new StringBuilder();
        K8sUtil.describeHeader(apiProvider, this, res, sb);

        if (res instanceof V1PersistentVolumeClaim persistentVolumeClaim) {
            sb.append("Access Modes:  ").append(persistentVolumeClaim.getSpec().getAccessModes()).append("\n");
            sb.append("Storage Class: ").append(persistentVolumeClaim.getSpec().getStorageClassName()).append("\n");
            sb.append("Status:        ").append(persistentVolumeClaim.getStatus().getPhase()).append("\n");
            sb.append("Volume Mode:   ").append(persistentVolumeClaim.getSpec().getVolumeMode()).append("\n");
            sb.append("Volume Name:   ").append(persistentVolumeClaim.getSpec().getVolumeName()).append("\n");
            sb.append("Capacity:      ").append(persistentVolumeClaim.getStatus().getCapacity()).append("\n");
        }
        K8sUtil.describeFooter(apiProvider, this, res, sb);
        return sb.toString();
    }

    @Override
    public void replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        // this is dangerous ... deny like delete!
        var body = Yaml.loadAs(yaml, V1PersistentVolumeClaim.class);
        apiProvider.getCoreV1Api().replaceNamespacedPersistentVolumeClaim(
                name,
                namespace,
                body,
                null, null, null, null
        );
    }

    @Override
    public Object delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        // this is dangerous ... deny!
        K8sUtil.checkDeleteAccess(securityService, K8s.PERSISTENT_VOLUME_CLAIM);
        return apiProvider.getCoreV1Api().deleteNamespacedPersistentVolumeClaim(name, namespace, null, null, null, null, null, null);
    }

    @Override
    public Object create(ApiProvider apiProvider, String yaml) throws ApiException {
        // this is dangerous ... deny! - or stupid?
        var body = Yaml.loadAs(yaml, V1PersistentVolumeClaim.class);
        return apiProvider.getCoreV1Api().createNamespacedPersistentVolumeClaim(
                body.getMetadata().getNamespace() == null ? "default" : body.getMetadata().getNamespace(),
                body,null, null, null, null);
    }

    @Override
    public V1PersistentVolumeClaimList createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException {
        return apiProvider.getCoreV1Api().listNamespacedPersistentVolumeClaim(namespace,null, null, null, null, null, null, null, null, null, null, null);
    }

    @Override
    public Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getCoreV1Api().listPersistentVolumeCall(null, null, null, null, null, null, null, null, null, null, true, new CallBackAdapter(LOGGER));
    }

    @Override
    public V1PersistentVolumeClaimList createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getCoreV1Api().listPersistentVolumeClaimForAllNamespaces(null, null, null, null, null, null, null, null, null, null, null);
    }

    @Override
    public Object patch(ApiProvider apiProvider, String namespace, String name, String patchString) throws ApiException {
        V1Patch patch = new V1Patch(patchString);
        return PatchUtils.patch(
                V1PersistentVolumeClaim.class,
                () -> apiProvider.getCoreV1Api().patchNamespacedPersistentVolumeClaimCall(
                        name,
                        namespace,
                        patch,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                V1Patch.PATCH_FORMAT_JSON_PATCH,
                apiProvider.getClient()
        );
    }

}
