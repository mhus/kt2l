package de.mhus.kt2l.nodes;

import com.google.gson.reflect.TypeToken;
import de.mhus.commons.util.MEventHandler;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.ui.MainView;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.Watch;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@Slf4j
public class ClusterNodeWatch extends ClusterBackgroundJob {

    @Autowired
    K8sService k8s;

    @Getter
    private MEventHandler<Watch.Response<V1Node>> eventHandler = new MEventHandler<>();
    private Thread watchThread;
    private ApiClient client;
    private CoreV1Api api;

    @Override
    public void close() {
        if (watchThread != null) {
            watchThread.interrupt();
            watchThread = null;
        }
    }

    @Override
    public void init(MainView mainView, String clusterId, String jobId) throws IOException {

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
            Watch<V1Node> watch = Watch.createWatch(
                    client,
                    call,
                    new TypeToken<Watch.Response<V1Node>>() {
                    }.getType());

            for (Watch.Response<V1Node> event : watch) {
                V1Node res = event.object;
                V1ObjectMeta meta = res.getMetadata();
                switch (event.type) {
                    case K8sUtil.WATCH_EVENT_ADDED:
                    case K8sUtil.WATCH_EVENT_MODIFIED:
                    case K8sUtil.WATCH_EVENT_DELETED:
                        LOGGER.debug(event.type + " : " + meta.getName() + " " + meta.getNamespace() + " " + meta.getCreationTimestamp() + " " + res.getStatus().getPhase() );
                        break;
                    default:
                        LOGGER.warn("Unknown event type: " + event.type);
                }
                eventHandler.fire(event);
            }
        } catch (ApiException e) {
            LOGGER.error("ApiException", e);
        }
    }

}
