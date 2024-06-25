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
import de.mhus.commons.io.Zip;
import de.mhus.commons.tools.MJson;
import de.mhus.commons.tools.MLang;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.CallBackAdapter;
import de.mhus.kt2l.k8s.K8sService;
import io.kubernetes.client.Metrics;
import io.kubernetes.client.custom.ContainerMetrics;
import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Pair;
import io.kubernetes.client.openapi.apis.ApiextensionsV1Api;
import io.kubernetes.client.openapi.apis.ApisApi;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1EphemeralContainer;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;
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
    public void getHelmInstallations() throws IOException, ApiException {
        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }

        final var service = new K8sService();
        ApiProvider apiProvider = service.getKubeClient(CLUSTER_NAME);
        //apiProvider.getClient().setDebugging(true);
        var list = apiProvider.getCoreV1Api().listSecretForAllNamespaces(
                null,
                null,
                "type=helm.sh/release.v1",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        list.getItems().forEach(secret -> {
            final var metadata = secret.getMetadata();
            final var name = metadata.getName();
            final var creationTimestamp = metadata.getCreationTimestamp();
            System.out.println(name + " " + creationTimestamp);
        });


        var first = list.getItems().get(0);
        var data = Base64.getDecoder().decode(first.getData().get("release"));
        var rawStream = new ByteArrayOutputStream();
        Zip.builder().srcStream(new ByteArrayInputStream(data)).dstStream(rawStream).build().ungzip();
        var jsonStr = new String(rawStream.toByteArray());
        var json = MJson.load(jsonStr);
        System.out.println(MJson.toPrettyString(json));

//        var templates = json.get("chart").get("templates");
//        for (int i = 0; i < templates.size(); i++) {
//            var template = templates.get(i);
//            var name = template.get("name").asText();
//            var dataStr = template.get("data").asText();
//            var dataBytes = Base64.getDecoder().decode(dataStr);
//            System.out.println("===============================");
//            System.out.println(name);
//            System.out.println("---------------------------");
//            System.out.println(new String(dataBytes));
//
//        }
    }

    @Test
    public void getEventsForPod() throws IOException, ApiException {
        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }

        final var service = new K8sService();
        ApiProvider apiProvider = service.getKubeClient(CLUSTER_NAME);

        final var podName = LOCAL_PROPERTIES.getProperty("pod.name");
        final var namespace = LOCAL_PROPERTIES.getProperty("namespace");
        final var pod = apiProvider.getCoreV1Api().readNamespacedPod(podName, namespace, null);
        System.out.println("Pod: " + pod.getMetadata().getName() + " " + pod.getMetadata().getCreationTimestamp() + " " + pod.getStatus().getPhase() + " " + pod.getStatus().getReason() + " " + pod.getStatus().getMessage() + " " + pod.getStatus().getStartTime());
        System.out.println("---------------------------------");
        final var uid = pod.getMetadata().getUid();
        final var fieldSelector = "involvedObject.uid=" + uid;
        // final var fieldSelector = "involvedObject.name=" + podName + ",involvedObject.namespace=" + namespace;
        final var list = apiProvider.getCoreV1Api().listNamespacedEvent(namespace, null, null, null, fieldSelector, null, 5, null, null, null, 10, null);
//        final var list = apiProvider.getCoreV1Api().listEventForAllNamespaces( null, null, null, fieldSelector, 10, null, null, null, null, 10, null);
        list.getItems().forEach(item -> {
            final var metadata = item.getMetadata();
            final var name = metadata.getName();
            final var creationTimestamp = metadata.getCreationTimestamp();
            final var involvedObject = item.getInvolvedObject();
            final var message = item.getMessage();
            final var reason = item.getReason();
            final var type = item.getType();
            final var firstTimestamp = item.getFirstTimestamp();
            final var lastTimestamp = item.getLastTimestamp();
            final var count = item.getCount();
            final var source = item.getSource();
            final var reportingComponent = item.getReportingComponent();
            final var reportingInstance = item.getReportingInstance();
            System.out.println(name + " " + creationTimestamp + " " + involvedObject + " " + message + " " + reason + " " + type + " " + firstTimestamp + " " + lastTimestamp + " " + count + " " + source + " " + reportingComponent + " " + reportingInstance);
        });
        System.out.println("---------------------------------");

    }

    @Test
    public void watchEvents() throws IOException, ApiException {
        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }
        final var service = new K8sService();
        ApiProvider apiProvider = service.getKubeClient(CLUSTER_NAME);

        final var eventCall = apiProvider.getCoreV1Api().listEventForAllNamespacesCall( null, null, null, null, null, null, null, null, null, null, true, new CallBackAdapter<CoreV1Event>(LOGGER));
        Watch<CoreV1Event> watch = Watch.createWatch(
                apiProvider.getClient(),
                eventCall,
                new TypeToken<Watch.Response<CoreV1Event>>(){}.getType());

        for (Watch.Response<CoreV1Event> event : watch) {
            System.out.println(event.type + " '" + event.object.getInvolvedObject().getName() + "' " + event.object.getMessage());
//            System.out.println(event.type + " " + event.object.toString().replace("\n", " "));
        }
    }


    @Test
    public void getNamespaces() throws IOException, ApiException {

        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }

        final var service = new K8sService();
        ApiProvider apiProvider = service.getKubeClient(CLUSTER_NAME);

        final var list = apiProvider.getCoreV1Api().listNamespace(null, null, null, null, null, null, null, null, null, null, null);
        list.getItems().forEach(item -> {
            final var metadata = item.getMetadata();
            final var name = metadata.getName();
            final var creationTimestamp = metadata.getCreationTimestamp();
            System.out.println(name + " " + creationTimestamp);
        });
    }

    @Test
    public void getNodes() throws IOException, ApiException {

        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }

        final var service = new K8sService();
        ApiProvider apiProvider = service.getKubeClient(CLUSTER_NAME);
        apiProvider.getClient().setDebugging(true);
        apiProvider.getCoreV1Api().listNode(null, null, null, null, null, null, null, null, null, null, null).getItems().forEach(node -> {
            final var metadata = node.getMetadata();
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
        apiProvider.getCoreV1Api().listNamespaceAsync(null, null, null, null, null, null, null, null, null, null, null, new ApiCallback<V1NamespaceList>() {
            @Override
            public void onFailure(ApiException e, int i, Map map) {
                LOGGER.warn("Failed to get namespaces: {}", e.getMessage());
                done.set(true);
            }

            @Override
            public void onSuccess(V1NamespaceList o, int i, Map map) {
                LOGGER.debug("Namespaces:");
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
                LOGGER.debug("Upload progress: {} {}", l, l1);
            }

            @Override
            public void onDownloadProgress(long l, long l1, boolean b) {
                LOGGER.trace("Download progress: {} {}", l, l1);
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

        var pod = apiProvider.getCoreV1Api().readNamespacedPod(podName, namespace, null);
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
        api.getApiClient().setDebugging(true);

        final var list = api.getAPIResources();

        final Map<String, V1APIResource> resources = new TreeMap<>();
        list.getResources().forEach(resource -> {
            resources.put(resource.getName(), resource);
        });
        resources.keySet().forEach(k -> System.out.println("core " + k) );

        // ---

        AppsV1Api appsApi = new AppsV1Api(apiProvider.getClient());
        appsApi.getAPIResources().getResources().forEach(resource -> {
            System.out.println("apps " + resource.getName());
        });

        // ---

        ApiextensionsV1Api apiextensionsApi = new ApiextensionsV1Api(apiProvider.getClient());
        apiextensionsApi.getAPIResources().getResources().forEach(resource -> {
            System.out.println("apiextensions " + resource.getName());
        });

        // ---

        ApisApi apisApi = new ApisApi(apiProvider.getClient());
        var v = apisApi.getAPIVersions();

        System.out.println("ApisVersion: " + v.getApiVersion());

        // ---


    }

    public Call getAPIResourcesCall(ApiCallback _callback, ApiClient client) throws ApiException {
        Object localVarPostBody = null;
        String localVarPath = "/api";
        List<Pair> localVarQueryParams = new ArrayList();
        List<Pair> localVarCollectionQueryParams = new ArrayList();
        Map<String, String> localVarHeaderParams = new HashMap();
        Map<String, String> localVarCookieParams = new HashMap();
        Map<String, Object> localVarFormParams = new HashMap();

        localVarHeaderParams.put("Accept", "application/json;g=apidiscovery.k8s.io;v=v2beta1;as=APIGroupDiscoveryList,application/json");
        localVarHeaderParams.put("User-Agent", "kubectl/v1.29.2 (darwin/arm64) kubernetes/4b8e819");

        String[] localVarContentTypes = new String[0];
        String localVarContentType = client.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);
        String[] localVarAuthNames = new String[]{"BearerToken"};
        return client.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
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

        var call = apiProvider.getCoreV1Api().listPodForAllNamespacesCall(null, null, null, null, null, null, null, null, null, null, true,new CallBackAdapter<V1Pod>(LOGGER));

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
////        String type = "apps/v1/daemonsets";
////        String type = "pods";
//        String type = "storage.k8s.io/v1/csidrivers";
//
//        // v1/pods
//        // apps/v1/daemonsets
//        // storage.k8s.io/v1/csidrivers
//        final var parts = type.split("/");
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
                apiProvider.getCoreV1Api().listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null, null);
        for (V1Pod item : list.getItems()) {
            System.out.println(item.getMetadata().getName());
        }
    }

    @Test
    public void testKubectl() throws KubectlException, IOException {
        if (CLUSTER_NAME == null) {
            LOGGER.error("Local properties not found");
            return;
        }
        final var service = new K8sService();
        ApiProvider apiProvider = service.getKubeClient(CLUSTER_NAME);
        var resources = Kubectl.apiResources().apiClient(apiProvider.getClient()).execute();

        resources.forEach(resource -> {
            System.out.println(resource.getResourceSingular() + " " + resource.getKind() + " " + resource.getGroup() + " " + resource.getVersions() + " " + resource.getNamespaced());
        });


    }

}
