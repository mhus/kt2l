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
package de.mhus.kt2l.resources.util;

import com.google.gson.reflect.TypeToken;
import de.mhus.commons.tools.MThread;
import de.mhus.commons.util.MEventHandler;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sService;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.Watch;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.lang.reflect.Type;

@Slf4j
public abstract class AbstractClusterWatch<V extends KubernetesObject> extends ClusterBackgroundJob {
    @Autowired
    K8sService k8s;

    @Getter
    private MEventHandler<Watch.Response<V>> eventHandler = new MEventHandler<>();
    private Thread watchThread;
    private String clusterId;
    private HandlerK8s resourceHandler;

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
        resourceHandler = k8s.getResourceHandler(getManagedResourceType());
    }

    public abstract K8s getManagedResourceType();

    private void watch() {

        while (true) {
            try {
                var apiProvider = k8s.getKubeClient(clusterId);

                okhttp3.Call call = createResourceCall(apiProvider);

                Watch<V> watch = Watch.createWatch(
                        apiProvider.getClient(),
                        call,
                        createTypeToken()
                        );

                for (Watch.Response<V> event : watch) {
                    if (event.object instanceof KubernetesObject) {
                        V res = event.object;
                        V1ObjectMeta meta = res.getMetadata();
                        switch (event.type) {
                            case K8sUtil.WATCH_EVENT_ADDED:
                            case K8sUtil.WATCH_EVENT_MODIFIED:
                            case K8sUtil.WATCH_EVENT_DELETED:
                                LOGGER.debug("➤ Event " + getClass().getSimpleName() + ": " + event.type + " - " + meta.getName() + " " + meta.getNamespace() + " " + meta.getCreationTimestamp());
                                break;
                            default:
                                LOGGER.warn("Unknown event type: " + event.type);
                        }
                        eventHandler.fire(event);
                    } else {
                        LOGGER.info("➤ Unknown object type: " + event.object.getClass());
                    }
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

    protected abstract Type createTypeToken();

    protected Call createResourceCall(ApiProvider apiProvider) throws ApiException {
        return resourceHandler.createResourceWatchCall(apiProvider);
    }

    protected void onError(Exception e) {
        MThread.sleep(1000); //XXX
    }

}
