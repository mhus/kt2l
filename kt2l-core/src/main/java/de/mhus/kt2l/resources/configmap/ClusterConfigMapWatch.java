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

package de.mhus.kt2l.resources.configmap;

import com.google.gson.reflect.TypeToken;
import de.mhus.commons.util.MEventHandler;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.k8s.K8s;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.Watch;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@Slf4j
public class ClusterConfigMapWatch extends ClusterBackgroundJob {

    @Autowired
    K8sService k8s;

    @Getter
    private MEventHandler<Watch.Response<V1ConfigMap>> eventHandler = new MEventHandler<>();
    private Thread watchThread;
    private ApiClient client;
    private CoreV1Api api;

    public static ClusterConfigMapWatch instance(Core core, Cluster clusterConfig) {
        return core.getBackgroundJob(clusterConfig.name(), ClusterConfigMapWatch.class, () -> new ClusterConfigMapWatch());
    }

    private ClusterConfigMapWatch() {
    }

    @Override
    public void close() {
        if (watchThread != null) {
            watchThread.interrupt();
            watchThread = null;
        }
    }

    @Override
    public void init(Core core, String clusterId, String jobId) throws IOException {

        client = k8s.getKubeClient(clusterId);
        api = new CoreV1Api(client);

        watchThread = Thread.startVirtualThread(this::watch);
        
    }

    private void watch() {

        try {
            var call = api.listNodeCall(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    true,
                    null);
            Watch<V1ConfigMap> watch = Watch.createWatch(
                    client,
                    call,
                    new TypeToken<Watch.Response<V1Node>>() {
                    }.getType());

            for (Watch.Response<V1ConfigMap> event : watch) {
                V1ConfigMap res = event.object;
                V1ObjectMeta meta = res.getMetadata();
                switch (event.type) {
                    case K8s.WATCH_EVENT_ADDED:
                    case K8s.WATCH_EVENT_MODIFIED:
                    case K8s.WATCH_EVENT_DELETED:
                        LOGGER.debug(event.type + " : " + meta.getName() + " " + meta.getNamespace() + " " + meta.getCreationTimestamp() );
                        break;
                    default:
                        LOGGER.warn("Unknown event type: " + event.type);
                }
                eventHandler.fire(event);
            }
        } catch (Exception e) {
            LOGGER.error("Exception", e);
        }
    }

}
