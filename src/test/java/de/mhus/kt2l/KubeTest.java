package de.mhus.kt2l;

import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1WatchEvent;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    @Disabled
    public void testWatch() throws IOException, ApiException {

        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }
        final var service = new K8sService();
        ApiClient client = service.getKubeClient(CLUSTER_NAME);

        CoreV1Api api = new CoreV1Api(client);

        Watch<V1WatchEvent> watch = Watch.createWatch(
                client,
//                api.listNamespaceCall(null,
//                        null,
//                        null,
//                        null,
//                        null,
//                        5,
//                        null,
//                        null,
//                        null, Boolean.TRUE, null),
                api.listEventForAllNamespacesCall(null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Boolean.TRUE,
                        null),
                new TypeToken<Watch.Response<V1WatchEvent>>(){}.getType());

        for (Watch.Response<V1WatchEvent> item : watch) {
            System.out.printf("%s : %s%n", item.type, item.object);
        }
    }

    @Test
    public void testListAllResources() throws IOException, ApiException {

        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }
        final var service = new K8sService();
        ApiClient client = service.getKubeClient(CLUSTER_NAME);

        AppsV1Api api = new AppsV1Api(client);
        GenericObjectsApi customApi = new GenericObjectsApi(client);
//        Object list = customApi.listClusterCustomObject("storage.k8s.io", "v1", "csidrivers", "true", null, null, null, null, null, null, null, null, null);
//        Object list = customApi.listNamespacedCustomObject("apps", "v1", "default", "daemonsets", null, null, null, null, null, null, null, null, null, null);
//        Object list = customApi.listNamespacedCustomObject("apps", "v1", "", "daemonsets", null, null, null, null, null, null, null, null, null, null);
        Object list = customApi.listNamespacedCustomObject(null, "v1", null, "pods", null, null, null, null, null, null, null, null, null, null);
        System.out.println(list.getClass());
        System.out.println(list);
    }

    @Test
    public void testGenericApi() throws IOException, ApiException {

        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }
        final var service = new K8sService();
        ApiClient client = service.getKubeClient(CLUSTER_NAME);

        AppsV1Api api = new AppsV1Api(client);
        GenericObjectsApi genericApi = new GenericObjectsApi(client);

//        String resourceType = "apps/v1/daemonsets";
//        String resourceType = "pods";
        String resourceType = "storage.k8s.io/v1/csidrivers";

        // v1/pods
        // apps/v1/daemonsets
        // storage.k8s.io/v1/csidrivers
        final var parts = resourceType.split("/");
        String group = null;
        String version = "v1";
        String plural = null;
        if (parts.length == 3) {
            group = parts[0];
            version = parts[1];
            plural = parts[2];
        } else if (parts.length == 2) {
            group = parts[0];
            plural = parts[1];
        } else {
            plural = parts[0];
        }

        final var list = genericApi.listNamespacedCustomObject(group, version, null, plural, null, null, null, null, null, null, null, null, null, null);
        final var type = new TypeToken<Map<String, Object>>() {
        }.getType();
        final var items = (List<Map<String, Object>>) ((LinkedTreeMap<String,Object>)list).get("items");
        items.forEach(item -> {
            final var metadata = (Map<String, Object>)((Map<String, Object>) item).get("metadata");
            final var name = (String) metadata.get("name");
            final var creationTimestamp = (String) metadata.get("creationTimestamp");
            //final var status = item.get("status").toString();

            System.out.println(name + " " + creationTimestamp);
        });
    }


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
