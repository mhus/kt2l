package de.mhus.kt2l.portforward;

import de.mhus.commons.crypt.MRandom;
import de.mhus.commons.lang.IRegistry;
import de.mhus.commons.services.MService;
import de.mhus.commons.tools.MThread;
import de.mhus.commons.util.MEventHandler;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.cluster.ClusterService;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.k8s.ApiProvider;
import io.kubernetes.client.PortForward;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class PortForwardBackgroundJob extends ClusterBackgroundJob {

    @Autowired
    ClusterService clusterService;

    private MEventHandler<PortForwardingChanged> eventHandler = new MEventHandler<>();

    private List<Forwarding> forwardings = Collections.synchronizedList(new LinkedList<>());
    private Cluster cluster;

    public Optional<Forwarding> getForwarding(String type, String namespace, String name, int servicePort, int localPort) {
        return forwardings.stream().filter(f -> f.type.equals(type) && f.namespace.equals(namespace) && f.name.equals(name) && f.servicePort == servicePort && f.localPort == localPort).findFirst();
    }

    public Forwarding addServiceForwarding(String namespace, String name, int servicePort, int localPort) {
        var forwarding = new ServiceForwarding(cluster.getApiProvider(), namespace, name, servicePort, localPort);
        if (forwardings.contains(forwarding)) {
            throw new IllegalArgumentException("Forwarding already exists");
        }
        forwardings.add(forwarding);
        return forwarding;
    }

    public Forwarding addPodForwarding(String namespace, String name, int servicePort, int localPort) {
        var forwarding = new PodForwarding(cluster.getApiProvider(), namespace, name, servicePort, localPort);
        if (forwardings.contains(forwarding)) {
            throw new IllegalArgumentException("Forwarding already exists");
        }
        forwardings.add(forwarding);
        return forwarding;
    }

    @Override
    public void close() {
        if (forwardings != null) {
            forwardings.forEach(f -> {
                f.close();
            });
        }
    }

    @Override
    public void init(Core core, String clusterId, String jobId) throws IOException {
        cluster = clusterService.getCluster(clusterId);
        // listen to avoid removing the background job
        eventHandler.register(this::onPortForwardingChanged);
    }

    private void onPortForwardingChanged(PortForwardingChanged portForwardingChanged) {
        // DUMMY
    }

    @Override
    public IRegistry<PortForwardingChanged> getEventHandler() {
        return eventHandler;
    }

    public void removeForwarding(Forwarding forwarding) {
        try {
            forwarding.close();
        } catch (Exception e) {
            LOGGER.error("Failed to close forwarding", e);
        }
        forwardings.remove(forwarding);
    }

    public List<Forwarding> getForwardings() {
        return List.copyOf(forwardings);
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

    public static class ServiceForwarding extends Forwarding {

        private final MRandom random;

        private ServiceForwarding(ApiProvider apiProvider, String namespace, String name, int servicePort, int localPort) {
            super(apiProvider, "svc", namespace, name, servicePort, localPort);
            random = MService.getService(MRandom.class);
        }

        @Override
        protected PortForward.PortForwardResult createForwarding() throws IOException, ApiException {
            var service =  apiProvider.getCoreV1Api().readNamespacedService(getName(), getNamespace(), null);
            if (service.getSpec().getSelector() == null)
                throw new ApiException("Service has no selector");

            var port = service.getSpec().getPorts().stream().filter(p -> p.getPort() == getServicePort()).findFirst().orElse(null);
            if (port == null)
                throw new ApiException("Service has no port " + getServicePort());

            var labelSelector = service.getSpec().getSelector().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).reduce((a, b) -> a + "," + b).orElse(null);
            var pods = apiProvider.getCoreV1Api().listNamespacedPod(getNamespace(), null, null, null, null, labelSelector, null, null, null, null, null, null);
            LOGGER.debug("Found {} pods for service {}/{}", pods.getItems().size(),getNamespace(),getName());
            if (pods.getItems().size() == 0)
                throw new ApiException("No pod found for service");

            var pod = pods.getItems().get(random.getInt() % pods.getItems().size());

            AtomicInteger targetPort = new AtomicInteger(-1);
            if (port.getTargetPort().isInteger())
                targetPort.set(port.getTargetPort().getIntValue());
            else {
                pod.getSpec().getContainers().forEach(c -> {
                    if (c.getPorts() != null) {
                        var p = c.getPorts().stream().filter(pp -> pp.getName().equals(port.getTargetPort().getStrValue())).findFirst().orElse(null);
                        if (p != null) {
                            targetPort.set(p.getContainerPort());
                        }
                    }
                });
            }
            if (targetPort.get() == -1)
                throw new ApiException("No target port found for service port " + getServicePort());

            LOGGER.debug("Selected pod {}/{} for service {}/{} on port {}", pod.getMetadata().getNamespace(), pod.getMetadata().getName(), getNamespace(), getName(), targetPort);
            return  portForward.forward(pod, List.of(targetPort.get()));
        }
    }

    public static class PodForwarding extends Forwarding {

        private PodForwarding(ApiProvider apiProvider, String namespace, String name, int servicePort, int localPort) {
            super(apiProvider, "pod", namespace, name, servicePort, localPort);
        }

        @Override
        protected PortForward.PortForwardResult createForwarding() throws IOException, ApiException {
            return  portForward.forward(getNamespace(), getName(), List.of(getServicePort()));
        }
    }

    public static abstract class Forwarding {

        @Getter
        private final String namespace;
        @Getter
        private final String name;
        @Getter
        private final int servicePort;
        @Getter
        protected final int localPort;
        private final String id;
        private final String type;
        protected final ApiProvider apiProvider;
        private ServerSocket socket;
        private Thread listenerThread;
        protected final PortForward portForward;
        private final List<Thread> threads = Collections.synchronizedList(new LinkedList<>());
        private AtomicLong tx = new AtomicLong();
        private AtomicLong rx = new AtomicLong();
        private boolean closed;

        private Forwarding(ApiProvider apiProvider, String type, String namespace, String name, int servicePort, int localPort) {
            this.type = type;
            id = type + ":" + localPort + "->" + namespace + "/" + name + ":" + servicePort;
            this.namespace = namespace;
            this.name = name;
            this.servicePort = servicePort;
            this.localPort = localPort;
            this.apiProvider = apiProvider;
            portForward = new PortForward(apiProvider.getClient());
        }

        public void start() {
            if (closed)
                throw new IllegalStateException("Forwarding is closed");
            if (listenerThread != null) return;
            tx.set(0);
            rx.set(0);
            listenerThread = Thread.startVirtualThread(() -> {
                    listen();
            });
        }

        public boolean isRunning() {
            return listenerThread != null;
        }

        public int currentConnections() {
            return threads.size() / 2;
        }

        private void listen() {

            LOGGER.info("Forwarding {}/{}:{} to localhost:{}", namespace, name, servicePort, localPort);
            try {
                socket = new ServerSocket(localPort);
                while(true) {
                    var connection = socket.accept();
                    LOGGER.info("Connection accepted on {}", localPort);
                    Thread.startVirtualThread(() -> {
                        startConnection(connection, servicePort);
                    });
                }
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    LOGGER.info("Forwarding interrupted");
                } else {
                    LOGGER.error("Forwarding failed", e);
                }
            }
            listenerThread = null;
        }

        private void startConnection(Socket connection, int servicePort) {
            try {
                LOGGER.debug("Forwarding connection on local {} -> {}", localPort, servicePort);
                var portForwardResult = createForwarding();
                threads.add(
                        Thread.startVirtualThread(() -> {
                                try {
                                    forwardStream("stdin", tx, connection.getInputStream(), portForwardResult.getOutboundStream(servicePort), connection);
                                } catch (IOException e) {
                                    LOGGER.error("Forwarding failed", e);
                                }
                            }));
                threads.add(
                        Thread.startVirtualThread(() -> {
                            try {
                                forwardStream("stdout", rx, portForwardResult.getInputStream(servicePort), connection.getOutputStream(), connection);
                            } catch (IOException e) {
                                LOGGER.error("Forwarding failed", e);
                            }
                        }));

            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    LOGGER.info("Forwarding interrupted");
                } else {
                    LOGGER.error("Forwarding failed", e);
                }
                try {
                    connection.close();
                } catch (IOException ex) {
                    LOGGER.error("Failed to close connection", ex);
                }
            }
        }

        protected abstract PortForward.PortForwardResult createForwarding() throws IOException, ApiException;

        private void forwardStream(String name, AtomicLong transfered, InputStream in, OutputStream out, Socket connection) {
            try {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    if (len == 0) {
                        MThread.sleep(100);
                        continue;
                    }
                    out.write(buffer, 0, len);
                    transfered.addAndGet(len);
                }
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    LOGGER.info("Forwarding interrupted");
                } else {
                    LOGGER.error("Forwarding failed for {}", name, e);
                }
                try {
                    connection.close();
                } catch (IOException ex) {
                    LOGGER.error("Failed to close connection", ex);
                }
            }
            threads.remove(Thread.currentThread());
        }

        public void stop() {
            if (listenerThread != null) {
                listenerThread.interrupt();
                listenerThread = null;
            }
            try {
                socket.close();
                socket = null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            threads.forEach(Thread::interrupt);
            threads.clear();
        }

        public void close() {
            stop();
            closed = true;
        }

        public boolean isClosed() {
            return closed;
        }

        public String toString() {
            return id;
        }

        public boolean equals(Object obj) {
            if (obj instanceof Forwarding) {
                return id.equals(((Forwarding)obj).id);
            }
            return false;
        }

        public int hashCode() {
            return id.hashCode();
        }

        public long getTx() {
            return tx.get();
        }

        public long getRx() {
            return rx.get();
        }
    }
}
