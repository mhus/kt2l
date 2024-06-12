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

package de.mhus.kt2l.resources.persistentvolume;

import de.mhus.commons.console.ConsoleTable;
import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.CallBackAdapter;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import io.kubernetes.client.openapi.models.V1PersistentVolumeList;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PersistentVolumeK8s implements HandlerK8s {

    @Autowired
    private SecurityService securityService;

    @Override
    public K8s getManagedResourceType() {
        return K8s.PERSISTENT_VOLUME;
    }

    @Override
    public String getDescribe(ApiProvider apiProvider, KubernetesObject res) {
        var sb = new StringBuilder();
        K8sUtil.describeHeader(apiProvider, this, res, sb);

        if (res instanceof V1PersistentVolume persistentVolume) {
            sb.append("Capacity:      ").append(persistentVolume.getSpec().getCapacity()).append("\n");
            sb.append("Access Modes:  ").append(persistentVolume.getSpec().getAccessModes()).append("\n");
            sb.append("Storage Class: ").append(persistentVolume.getSpec().getStorageClassName()).append("\n");
            sb.append("Status:        ").append(persistentVolume.getStatus().getPhase()).append("\n");
            sb.append("Claim:         ").append(persistentVolume.getSpec().getClaimRef()).append("\n");
            sb.append("Reclaim Policy:").append(persistentVolume.getSpec().getPersistentVolumeReclaimPolicy()).append("\n");
            sb.append("Mount Options: ").append(persistentVolume.getSpec().getMountOptions()).append("\n");
            sb.append("Volume Mode:   ").append(persistentVolume.getSpec().getVolumeMode()).append("\n");
            sb.append("Storage:       ").append(persistentVolume.getSpec().getCapacity()).append("\n");
        }
        K8sUtil.describeFooter(apiProvider, this, res, sb);
        return sb.toString();
    }

    @Override
    public void replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException {
        // this is dangerous ... deny like delete!
        var body = Yaml.loadAs(yaml, V1PersistentVolume.class);
        apiProvider.getCoreV1Api().replacePersistentVolume(
                name,
                body,
                null, null, null, null
        );
    }

    @Override
    public Object delete(ApiProvider apiProvider, String name, String namespace) throws ApiException {
        // this is dangerous ... deny!
        K8sUtil.checkDeleteAccess(securityService, K8s.PERSISTENT_VOLUME);
        return apiProvider.getCoreV1Api().deletePersistentVolume(name, null, null, null, null, null, null );
    }

    @Override
    public Object create(ApiProvider apiProvider, String yaml) throws ApiException {
        // this is dangerous ... deny! - or stupid?
        var body = Yaml.loadAs(yaml, V1PersistentVolume.class);
        return apiProvider.getCoreV1Api().createPersistentVolume(body,null, null, null, null);
    }

    @Override
    public <L extends KubernetesListObject> L createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException {
        throw new NotImplementedException();
    }

    @Override
    public Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getCoreV1Api().listPersistentVolumeCall(null, null, null, null, null, null, null, null, null, null, true, new CallBackAdapter(LOGGER));
    }

    @Override
    public V1PersistentVolumeList createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException {
        return apiProvider.getCoreV1Api().listPersistentVolume(null, null, null, null, null, null, null, null, null, null, null);
    }
}
