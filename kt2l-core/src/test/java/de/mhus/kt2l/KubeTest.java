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

package de.mhus.kt2l;

import com.google.gson.reflect.TypeToken;
import de.mhus.kt2l.k8s.GenericObjectsApi;
import de.mhus.kt2l.k8s.K8sService;
import io.kubernetes.client.Metrics;
import io.kubernetes.client.custom.ContainerMetrics;
import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.CoreV1EventList;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1EphemeralContainer;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
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
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;

@Slf4j
@Disabled
public class KubeTest {

    private static final Properties LOCAL_PROPERTIES = new Properties();
    static {
        try {
            LOCAL_PROPERTIES.load(KubeTest.class.getClassLoader().getResourceAsStream("local.properties"));
        } catch (Exception e) {
            LOGGER.error("Failed to load local.properties: {}", e.getMessage());
        }
    }
    private static final String CLUSTER_NAME = LOCAL_PROPERTIES.getProperty("cluster.name", null);

    @Test
    public void testWatchEvents2() throws IOException, ApiException {

        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }
        final var service = new K8sService();
        ApiClient client = service.getKubeClient(CLUSTER_NAME);

        CoreV1Api api = new CoreV1Api(client);

        CoreV1EventList events=api.listNamespacedEvent("default", "no_pretty", null, null, null, null, null, null, null, null, true);
        String rev=events.getMetadata().getResourceVersion();
        Watch<V1WatchEvent> watch = Watch.createWatch(
                client,
                api.listEventForAllNamespacesCall(null, null, null, null, null, "no_pretty", null, rev,
                        null, true, null),
                new TypeToken<Watch.Response<CoreV1EventList>>(){}.getType());
// iterate over the response
        for (Watch.Response<V1WatchEvent> item : watch) {
            System.out.printf("Object: %s, Type %s, Status %s\n",
                    item.object,
                    item.type,
                    item.status);
        }

    }

    @Test
    public void testPodDebug() throws IOException, ApiException {

        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }

        final var service = new K8sService();
        ApiClient client = service.getKubeClient(CLUSTER_NAME);

        CoreV1Api api = new CoreV1Api(client);

        final var podName = LOCAL_PROPERTIES.getProperty("pod.name");
        final var namespace = LOCAL_PROPERTIES.getProperty("namespace");

        var debugger = new V1EphemeralContainer();
        debugger.setName("debugger-" + UUID.randomUUID());
        debugger.setImage("nginx");
//        V1Patch patch = new V1Patch(debugger    );

//        api.patchNamespacedPodEphemeralcontainers(name, namespace, )

    }

    @Test
    public void testGetPod() throws IOException, ApiException {

        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }

        final var service = new K8sService();
        ApiClient client = service.getKubeClient(CLUSTER_NAME);

        CoreV1Api api = new CoreV1Api(client);

        final var podName = LOCAL_PROPERTIES.getProperty("pod.name");
        final var namespace = LOCAL_PROPERTIES.getProperty("namespace");

        var pod = api.readNamespacedPod(podName, namespace, null);
        System.out.println(pod);

        Metrics metrics = new Metrics(client);
        var list = metrics.getPodMetrics(namespace);
        for(PodMetrics podMetrics:list.getItems()){
            System.out.println("=== " + podMetrics.getMetadata().getName());
            for(ContainerMetrics containerMetrics:podMetrics.getContainers()){
                System.out.println(containerMetrics.getUsage());
            }
        }
    }


    @Test
    public void testWatchMetrics() throws IOException, ApiException {

        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }

        final var service = new K8sService();
        ApiClient client = service.getKubeClient(CLUSTER_NAME);

        CoreV1Api api = new CoreV1Api(client);

        Metrics metrics = new Metrics(client);

    }

    @Test
    public void testGetAllApiResourceTypes() throws IOException, ApiException {

        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }
        final var service = new K8sService();
        ApiClient client = service.getKubeClient(CLUSTER_NAME);

        CoreV1Api api = new CoreV1Api(client);

        final var list = api.getAPIResources();

        final Map<String, V1APIResource> resources = new TreeMap<>();
        list.getResources().forEach(resource -> {
            resources.put(resource.getName(), resource);
        });
        resources.keySet().forEach(k -> System.out.println(k) );

    }

    @Test
    public void testWatchEvents() throws IOException, ApiException {

        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }
        final var service = new K8sService();
        ApiClient client = service.getKubeClient(CLUSTER_NAME);

        CoreV1Api api = new CoreV1Api(client);

        Watch<V1WatchEvent> watch = Watch.createWatch(
                client,
                api.listEventForAllNamespacesCall(null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        120,
                        Boolean.TRUE,
                        null),
                new TypeToken<Watch.Response<V1WatchEvent>>(){}.getType());

        for (Watch.Response<V1WatchEvent> item : watch) {
            System.out.printf("%s : %s %s%n", item.type, item.object, item.status);
        }
    }

    @Test
    @Disabled
    // https://www.baeldung.com/java-kubernetes-watch
    public void testWatchPods() throws IOException, ApiException {

        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }
        final var service = new K8sService();
        ApiClient client = service.getKubeClient(CLUSTER_NAME);

        CoreV1Api api = new CoreV1Api(client);
        var call = api.listPodForAllNamespacesCall(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                120,
                true,
                null);
        Watch<V1Pod> watch = Watch.createWatch(
                client,
                call,
                new TypeToken<Watch.Response<V1Pod>>(){}.getType());

        for (Watch.Response<V1Pod> event : watch) {
            V1Pod pod = event.object;
            V1ObjectMeta meta = pod.getMetadata();
            switch (event.type) {
                case "ADDED":
                case "MODIFIED":
                case "DELETED":
                    System.out.println(event.type + " : " + meta.getName() + " " + meta.getNamespace() + " " + meta.getCreationTimestamp() + " " + pod.getStatus().getPhase() + " " + pod.getStatus().getReason() + " " + pod.getStatus().getMessage() + " " + pod.getStatus().getStartTime() + " " + pod.getStatus().getContainerStatuses());
                    break;
                default:
                    System.out.println("Unknown event type: " + event.type);
            }
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

        final var items = genericApi.listNamespacedCustomObject(group, version, null, plural, null, null, null, null, null, null, null, null, null, null);
        final var type = new TypeToken<Map<String, Object>>() {
        }.getType();
        items.forEach(item -> {
            final var metadata = item.getMetadata();
            final var name = metadata.getName();
            final var creationTimestamp = metadata.getCreationTimestamp();
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
        service.getAvailableContexts().forEach(LOGGER::info);
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
