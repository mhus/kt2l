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
import de.mhus.kt2l.k8s.K8sService;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodBuilder;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.StringReader;
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
        createNamespace("aremorica");
        createPod("idefix", "aremorica", "nginx:latest");

        waitForPodReady("idefix", "aremorica");
    }

    private static void waitForPodReady(String name, String namespace) {

        MLang.tryThis(() -> {
            while (true) {
                V1Pod pod = api.readNamespacedPod(name, namespace, null);
                if (pod.getStatus().getPhase().equals("Running")) {
                    break;
                }
                MThread.sleep(1000);
            }
        }).onError(e -> LOGGER.error("Error on wait", e));

    }

    private static V1Namespace createNamespace(String name) throws ApiException {
        V1Namespace namespace = new V1Namespace();
        V1ObjectMeta meta = new V1ObjectMeta();
        meta.name(name);
        namespace.metadata(meta);
        return api.createNamespace(namespace, null, null, null, null);
    }

    private static V1Pod createPod(String name, String namespace, String image) throws ApiException {

        var pod = new V1PodBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(namespace)
                    .addToLabels("app", name)
                .endMetadata()
                .withNewSpec()
                    .addNewContainer()
                        .withName(name)
                        .withImage(image)
                        .withImagePullPolicy("IfNotPresent")
                    .endContainer()
                .endSpec()
                .build();

        return api.createNamespacedPod(namespace, pod, null, null, null, null);
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