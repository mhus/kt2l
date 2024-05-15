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

package de.mhus.kt2l.resources.secret;

import com.google.gson.reflect.TypeToken;
import de.mhus.commons.util.MEventHandler;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.k8s.CallBackAdapter;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sService;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.util.Watch;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@Slf4j
public class SecretWatch extends ClusterBackgroundJob {

    @Autowired
    K8sService k8s;

    @Getter
    private MEventHandler<Watch.Response<V1Secret>> eventHandler = new MEventHandler<>();
    private Thread watchThread;
    private String clusterId;

    public static SecretWatch instance(Core core, Cluster cluster) {
        return core.getBackgroundJob(cluster.getName(), SecretWatch.class, () -> new SecretWatch());
    }

    private SecretWatch() {
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
        this.clusterId = clusterId;
        watchThread = Thread.startVirtualThread(this::watch);
        
    }

    private void watch() {
        while (true) {
            try {
                var apiProvider = k8s.getKubeClient(clusterId);

                var call = apiProvider.getCoreV1Api().listSecretForAllNamespaces().watch(true).buildCall(new CallBackAdapter<V1Secret>(LOGGER));
                Watch<V1Secret> watch = Watch.createWatch(
                        apiProvider.getClient(),
                        call,
                        new TypeToken<Watch.Response<V1Secret>>() {
                        }.getType());

                for (Watch.Response<V1Secret> event : watch) {
                    V1Secret res = event.object;
                    V1ObjectMeta meta = res.getMetadata();
                    switch (event.type) {
                        case K8s.WATCH_EVENT_ADDED:
                        case K8s.WATCH_EVENT_MODIFIED:
                        case K8s.WATCH_EVENT_DELETED:
                            LOGGER.debug(event.type + " : " + meta.getName() + " " + meta.getNamespace() + " " + meta.getCreationTimestamp());
                            break;
                        default:
                            LOGGER.warn("Unknown event type: " + event.type);
                    }
                    eventHandler.fire(event);
                }
            } catch (Exception e) {
                if (Thread.interrupted()) {
                    LOGGER.debug("Interrupted");
                    return;
                }
                LOGGER.error("Exception", e);
            }
        }
    }

}