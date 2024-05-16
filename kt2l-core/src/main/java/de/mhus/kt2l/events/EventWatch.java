package de.mhus.kt2l.events;

import com.google.gson.reflect.TypeToken;
import de.mhus.commons.lang.IRegistry;
import de.mhus.commons.util.MEventHandler;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.k8s.K8sUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@Slf4j
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

                okhttp3.Call call = apiProvider.getCoreV1Api().listEventForAllNamespacesCall(null, null, null, null, null, null, null, null, null, null, true, null);

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

    @Override
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
