package de.mhus.kt2l.portforward;

import de.mhus.commons.lang.IRegistry;
import de.mhus.commons.util.MEventHandler;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.Core;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.Watch;

import java.io.IOException;

public class PortForwardBackgroundJob extends ClusterBackgroundJob {

    private MEventHandler<PortForwardingChanged> eventHandler = new MEventHandler<>();

    @Override
    public void close() {
    }

    @Override
    public void init(Core core, String clusterId, String jobId) throws IOException {
        // listen to avoid removing the background job
        eventHandler.register(this::onPortForwardingChanged);
    }

    private void onPortForwardingChanged(PortForwardingChanged portForwardingChanged) {
    }

    @Override
    public IRegistry<PortForwardingChanged> getEventHandler() {
        return eventHandler;
    }

    public class PortForwardingChanged implements KubernetesObject {

        @Override
        public V1ObjectMeta getMetadata() {
            return null;
        }

        @Override
        public String getApiVersion() {
            return "";
        }

        @Override
        public String getKind() {
            return "";
        }
    }
}
