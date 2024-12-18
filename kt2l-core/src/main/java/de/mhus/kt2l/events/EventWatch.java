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
package de.mhus.kt2l.events;

import com.google.gson.reflect.TypeToken;
import de.mhus.commons.util.MEventHandler;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.k8s.K8sService;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.io.IOException;

@Slf4j
@Configurable
public class EventWatch extends ClusterBackgroundJob {

    @Autowired
    K8sService k8s;

    private String clusterId;
    private Thread watchThread;
    private MEventHandler<Watch.Response<CoreV1Event>> eventHandler = new MEventHandler<>();

    @Override
    public void init(Core core, String clusterId, String jobId) throws IOException {
        this.clusterId = clusterId;
        watchThread = Thread.startVirtualThread(this::watch);
    }

    private void watch() {

        while (true) {
            try {
                var apiProvider = k8s.getKubeClient(clusterId);

                okhttp3.Call call = apiProvider.getCoreV1Api().listEventForAllNamespaces().watch(true).buildCall(null);

                Watch<CoreV1Event> watch = Watch.createWatch(
                        apiProvider.getClient(),
                        call,
                        new TypeToken<Watch.Response<CoreV1Event>>() {
                        }.getType());

                for (Watch.Response<CoreV1Event> event : watch) {
                    LOGGER.debug("➤ Event: {} {} {} {}", event.object.getType(), event.object.getReason(), event.object.getInvolvedObject().getName(), event.object.getMessage());
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

    public MEventHandler<Watch.Response<CoreV1Event>> getEventHandler() {
        return eventHandler;
    }

    @Override
    public void close() {
        if (watchThread != null) {
            watchThread.interrupt();
            watchThread = null;
        }
    }

}
