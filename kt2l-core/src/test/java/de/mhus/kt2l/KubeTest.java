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
import de.mhus.commons.tools.MDate;
import de.mhus.commons.tools.MLang;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.CallBackAdapter;
import de.mhus.kt2l.k8s.K8sService;
import io.kubernetes.client.Metrics;
import io.kubernetes.client.custom.ContainerMetrics;
import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
    public void getNamespaces() throws IOException, ApiException {

        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }

        final var service = new K8sService();
        ApiProvider apiProvider = service.getKubeClient(CLUSTER_NAME);

        final var list = apiProvider.getCoreV1Api().listNamespace().execute();
        list.getItems().forEach(item -> {
            final var metadata = item.getMetadata();
            final var name = metadata.getName();
            final var creationTimestamp = metadata.getCreationTimestamp();
            System.out.println(name + " " + creationTimestamp);
        });
    }

    @Test
    public void getNamespacesAsync() throws IOException, ApiException {

        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }

        final var service = new K8sService();
        ApiProvider apiProvider = service.getKubeClient(CLUSTER_NAME);

        AtomicBoolean done = new AtomicBoolean(false);
        apiProvider.getCoreV1Api().listNamespace().executeAsync(new ApiCallback<V1NamespaceList>() {
            @Override
            public void onFailure(ApiException e, int i, Map map) {
                LOGGER.error("Failed to get namespaces: {}", e.getMessage());
                done.set(true);
            }

            @Override
            public void onSuccess(V1NamespaceList o, int i, Map map) {
                LOGGER.info("Namespaces:");
                o.getItems().forEach(item -> {
                    final var metadata = item.getMetadata();
                    final var name = metadata.getName();
                    final var creationTimestamp = metadata.getCreationTimestamp();
                    LOGGER.info(name + " " + creationTimestamp);
                });
                done.set(true);
            }

            @Override
            public void onUploadProgress(long l, long l1, boolean b) {
                LOGGER.info("Upload progress: {} {}", l, l1);
            }

            @Override
            public void onDownloadProgress(long l, long l1, boolean b) {
                LOGGER.info("Download progress: {} {}", l, l1);
            }
        });

        MLang.await(() -> done.get() ? "yo" : null, 10000, 1000);

    }
    @Test
    public void testPodDebug() throws IOException, ApiException {

        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }

        final var service = new K8sService();
        ApiProvider apiProvider = service.getKubeClient(CLUSTER_NAME);

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
        ApiProvider apiProvider = service.getKubeClient(CLUSTER_NAME);

        final var podName = LOCAL_PROPERTIES.getProperty("pod.name");
        final var namespace = LOCAL_PROPERTIES.getProperty("namespace");

        var pod = apiProvider.getCoreV1Api().readNamespacedPod(podName, namespace).execute();
        System.out.println(pod);

        Metrics metrics = new Metrics(apiProvider.getClient());
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
        ApiProvider apiProvider = service.getKubeClient(CLUSTER_NAME);

        CoreV1Api api = new CoreV1Api(apiProvider.getClient());

        Metrics metrics = new Metrics(apiProvider.getClient());

    }

    @Test
    public void testGetAllApiResourceTypes() throws IOException, ApiException {

        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }
        final var service = new K8sService();
        ApiProvider apiProvider = service.getKubeClient(CLUSTER_NAME);

        CoreV1Api api = new CoreV1Api(apiProvider.getClient());

        final var list = api.getAPIResources().execute();

        final Map<String, V1APIResource> resources = new TreeMap<>();
        list.getResources().forEach(resource -> {
            resources.put(resource.getName(), resource);
        });
        resources.keySet().forEach(k -> System.out.println(k) );

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
        ApiProvider apiProvider = service.getKubeClient(CLUSTER_NAME);

        var call = apiProvider.getCoreV1Api().listPodForAllNamespaces().watch(true).buildCall(new CallBackAdapter<V1Pod>(LOGGER));

        Watch<V1Pod> watch = Watch.createWatch(
                apiProvider.getClient(),
                call,
                new TypeToken<Watch.Response<V1Pod>>(){}.getType());

        for (Watch.Response<V1Pod> event : watch) {
            V1Pod pod = event.object;
            V1ObjectMeta meta = pod.getMetadata();
            switch (event.type) {
                case "ADDED":
                case "MODIFIED":
                case "DELETED":
                    //System.out.println(event.type + " : " + pod );
                    System.out.println(event.type + " : " + meta.getName() + " " + meta.getNamespace() + " " + meta.getCreationTimestamp() + " " + pod.getStatus().getPhase() + " " + pod.getStatus().getReason() + " " + pod.getStatus().getMessage() + " " + pod.getStatus().getStartTime() + " " + pod.getStatus().getContainerStatuses());
                    break;
                default:
                    System.out.println("Unknown event type: " + event.type);
            }
        }

    }

//    @Test
//    public void testGenericApi() throws IOException, ApiException {
//
//        if (CLUSTER_NAME == null) {
//            LOGGER.error("Local properties not found");
//            return;
//        }
//        final var service = new K8sService();
//        ApiClient client = service.getKubeClient(CLUSTER_NAME);
//
//        AppsV1Api api = new AppsV1Api(client);
//        GenericObjectsApi genericApi = new GenericObjectsApi(client);
//
////        String resourceType = "apps/v1/daemonsets";
////        String resourceType = "pods";
//        String resourceType = "storage.k8s.io/v1/csidrivers";
//
//        // v1/pods
//        // apps/v1/daemonsets
//        // storage.k8s.io/v1/csidrivers
//        final var parts = resourceType.split("/");
//        String group = null;
//        String version = "v1";
//        String plural = null;
//        if (parts.length == 3) {
//            group = parts[0];
//            version = parts[1];
//            plural = parts[2];
//        } else if (parts.length == 2) {
//            group = parts[0];
//            plural = parts[1];
//        } else {
//            plural = parts[0];
//        }
//
//        final var items = genericApi.listNamespacedCustomObject(group, version, null, plural, null, null, null, null, null, null, null, null, null, null);
//        final var type = new TypeToken<Map<String, Object>>() {
//        }.getType();
//        items.forEach(item -> {
//            final var metadata = item.getMetadata();
//            final var name = metadata.getName();
//            final var creationTimestamp = metadata.getCreationTimestamp();
//            //final var status = item.get("status").toString();
//
//            System.out.println(name + " " + creationTimestamp);
//        });
//    }


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
        ApiProvider apiProvider = service.getKubeClient(CLUSTER_NAME);
        LOGGER.info("Client: {}", apiProvider.getClient());
        LOGGER.info("Api: {}", apiProvider.getCoreV1Api());
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
        ApiProvider apiProvider = service.getKubeClient("*");
        LOGGER.info("Client: {}", apiProvider.getClient());
        LOGGER.info("Api: {}", apiProvider.getCoreV1Api());
    }

    @Test
    public void testKubeServiceApiAccess() throws IOException, ApiException {
        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }
        final var service = new K8sService();
        ApiProvider apiProvider = service.getKubeClient(CLUSTER_NAME);
        LOGGER.info("Api: {}", apiProvider.getCoreV1Api());
        V1PodList list =
                apiProvider.getCoreV1Api().listPodForAllNamespaces().execute();
        for (V1Pod item : list.getItems()) {
            System.out.println(item.getMetadata().getName());
        }    }

}
