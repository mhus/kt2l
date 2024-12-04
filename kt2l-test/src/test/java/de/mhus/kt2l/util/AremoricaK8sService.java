/*
 * kt2l-test - kt2l integration tests
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
package de.mhus.kt2l.util;

import de.mhus.commons.tools.MLang;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.K8sService;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceBuilder;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodBuilder;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static de.mhus.kt2l.DebugTestUtil.TEST_DEBUG;

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
        api.getApiClient().setDebugging(TEST_DEBUG);
    }

    public static void createAremorica() throws ApiException {
        createNamespace("indomitable-village");
        createPod("asterix", "indomitable-village", "mhus/example-dice:20240503", "INFINITE","true");

        waitForPodReady("asterix", "indomitable-village");
    }

    public static void waitForPodReady(String name, String namespace) {

        MLang.tryThis(() -> {
            while (true) {
                V1Pod pod = api.readNamespacedPod(name, namespace).execute();
                if (pod.getStatus().getPhase().equals("Running")) {
                    break;
                }
                MThread.sleep(1000);
            }
        }).onFailure(e -> LOGGER.error("Error on wait", e));

    }

    public static V1Namespace createNamespace(String name) throws ApiException {
        LOGGER.info("Create namespace: {}", name);

        V1Namespace namespace = new V1NamespaceBuilder()
                .withNewMetadata()
                    .withName(name)
                .endMetadata()
                .build();
        return api.createNamespace(namespace).execute();
    }

    public static V1Status deleteNamespace(String name) throws ApiException {
        LOGGER.info("Delete namespace: {}", name);
        return api.deleteNamespace(name).execute();
    }

    public static V1Pod createPod(String name, String namespace, String image, String ... env) throws ApiException {
        LOGGER.info("Create pod: {} in namespace: {}", name, namespace);
        List<V1EnvVar> envList = new ArrayList<>();
        for (int i = 0; i < env.length; i += 2) {
            envList.add(new V1EnvVar().name(env[i]).value(env[i+1]));
        }
        var pod = new V1PodBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(namespace)
                    .addToLabels("app", name)
                .endMetadata()
                .withNewSpec()
                    .withOverhead(null)
                    .addNewContainer()
                        .withName(name)
                        .withImage(image)
                        .withEnv(envList)
                        .withImagePullPolicy("IfNotPresent")
                    .endContainer()
                .withTerminationGracePeriodSeconds(2L)
                .endSpec()
                .build();

        api.getApiClient().setDebugging(true);
        return api.createNamespacedPod(namespace, pod).execute();
    }

    public static void stop() {
        MLang.tryThis(() -> k3s.stop()).onFailure(e -> LOGGER.error("Error on quit", e));
    }

    public static V1Pod deletePod(String name, String namespace) throws ApiException {
        return api.deleteNamespacedPod(name, namespace).execute();
    }

    @Override
    public Set<String> getAvailableContexts() {
        return Set.of("aremorica");
    }

    @Override
    public ApiProvider getKubeClient(String contextName, long timeout) throws IOException {
        if ("aremorica".equals(contextName)) {
            return new ApiProvider(timeout) {
                @Override
                protected ApiClient createClient() throws IOException {
                    return Config.fromConfig(new StringReader(k3s.getKubeConfigYaml()));
                }
            };
        }
        throw new IOException("Context not found");
    }



}