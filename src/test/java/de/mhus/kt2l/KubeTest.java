package de.mhus.kt2l;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.KubeConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Properties;

@Slf4j
public class KubeTest {

    private static final Properties LOCAL_PROPERTIES = new Properties();
    static {
        try {
            LOCAL_PROPERTIES.load(KubeTest.class.getClassLoader().getResourceAsStream("local.properties"));
        } catch (Exception e) {
            LOGGER.error("Failed to load local.properties", e);
        }
    }
    private static final String CLUSTER_NAME = LOCAL_PROPERTIES.getProperty("cluster.name", null);

    @Test
    public void testKubeConfigStructure() throws IOException {
        try (final var kubeConfigFile = new FileReader(System.getenv("HOME") + "/.kube/config")) {
            var kubeConfig = KubeConfig.loadKubeConfig(kubeConfigFile);
            kubeConfig.getContexts().forEach(
                    context -> LOGGER.info("Context: {}", ((LinkedHashMap)context).get("name") )
            );
        }
    }

    @Test
    public void testKubeServiceAvailableContexts() {
        final var service = new K8sService();
        service.availableContexts().forEach(LOGGER::info);
    }

    @Test
    public void testKubeServiceApi() throws IOException {
        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }
        final var service = new K8sService();
        ApiClient client = service.getKubeClient(CLUSTER_NAME);
        LOGGER.info("Client: {}", client);
        CoreV1Api api = service.getCoreV1Api(CLUSTER_NAME);
        LOGGER.info("Api: {}", api);
    }

    @Test
    public void testKubeServiceApiLocal() throws IOException {
//        final var service = new K8sService();
//        ApiClient client = service.getKubeClient("local-cluster");
//        LOGGER.info("Client: {}", client);
//        CoreV1Api api = service.getCoreV1Api("local-cluster");
//        LOGGER.info("Api: {}", api);
    }

    @Test
    public void testKubeServiceApiDefault() throws IOException {
        final var service = new K8sService();
        ApiClient client = service.getKubeClient("*");
        LOGGER.info("Client: {}", client);
        CoreV1Api api = service.getCoreV1Api("*");
        LOGGER.info("Api: {}", api);
    }

    @Test
    public void testKubeServiceApiAccess() throws IOException, ApiException {
        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }
        final var service = new K8sService();
        CoreV1Api api = service.getCoreV1Api(CLUSTER_NAME);
        LOGGER.info("Api: {}", api);
        V1PodList list =
                api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null,  null);
        for (V1Pod item : list.getItems()) {
            System.out.println(item.getMetadata().getName());
        }    }

}
