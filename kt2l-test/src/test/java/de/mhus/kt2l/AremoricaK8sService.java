package de.mhus.kt2l;

import de.mhus.commons.tools.MLang;
import de.mhus.kt2l.k8s.K8sService;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreApi;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerPort;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class AremoricaK8sService extends K8sService {

    public static K3sContainer k3s;
    private static ApiClient client;
    private static CoreV1Api api;

    public static void start() throws IOException {
        AremoricaK8sService.k3s = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.21.3-k3s1"))
                .withLogConsumer(new Slf4jLogConsumer(LOGGER));
        AremoricaK8sService.k3s.start();
        String kubeConfigYaml = AremoricaK8sService.k3s.getKubeConfigYaml();
        client = Config.fromConfig(new StringReader(kubeConfigYaml));
        api = new CoreV1Api(client);
    }

    public static void createAremorica() throws ApiException {
        createNamespace("aremorica");
        //TODO createPod("idefix", "aremorica", "nginx:latest");
    }

    private static V1Namespace createNamespace(String name) throws ApiException {
        V1Namespace namespace = new V1Namespace();
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.name(name);
        namespace.metadata(meta);
        return api.createNamespace(namespace, null, null, null, null);
    }

    private static V1Pod createPod(String name, String namespace, String image) throws ApiException {
        var pod = new V1Pod();

        V1ObjectMeta meta = new V1ObjectMeta();

        meta.name(name);
        meta.namespace(namespace);
        Map<String, String> labels = new HashMap<>();
        labels.put("app", name);
        meta.labels(labels);
//        V1ContainerPort port = new V1ContainerPort();
//        port.containerPort(8080);
        V1Container container = new V1Container();
        container.name(name);
        container.image(image);
        container.imagePullPolicy("IfNotPresent");
        container.ports(Arrays.asList());
        container.setEnv(Arrays.asList());

        V1PodSpec spec = new V1PodSpec();
        spec.containers(Arrays.asList(container));
        V1Pod podBody = new V1Pod();

        podBody.apiVersion("v1");
        podBody.kind("Pod");
        podBody.metadata(meta);
        podBody.spec(spec);

        Map<String, String> nodeSelectorMap = new HashMap<>();
        nodeSelectorMap.put("nodeLabelKey", "nodeLabelValue");
        spec.nodeSelector(nodeSelectorMap);

        return api.createNamespacedPod(namespace, podBody, null, null, null, null);
    }

    public static void stop() {
        MLang.tryThis(() -> k3s.stop()).onError(e -> LOGGER.error("Error on quit", e));
    }

    public Set<String> getAvailableContexts() {
        return Set.of("aremorica");
    }

    public ApiClient getKubeClient(String contextName) throws IOException {
        if ("aremorica".equals(contextName)) {
            return Config.fromConfig(new StringReader(k3s.getKubeConfigYaml()));
        }
        throw new IOException("Context not found");
    }

}