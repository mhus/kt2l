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

package de.mhus.kt2l.k8s;

import de.mhus.commons.errors.NotFoundRuntimeException;
import de.mhus.kt2l.config.AaaConfiguration;
import de.mhus.kt2l.core.SecurityContext;
import de.mhus.kt2l.core.SecurityService;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static io.kubernetes.client.util.Config.ENV_KUBECONFIG;
import static io.kubernetes.client.util.Config.SERVICEACCOUNT_CA_PATH;

@Slf4j
@Component
public class K8sService {

    public static final String LOCAL_CLUSTER_NAME = ".local-cluster";
    public static final String DEFAULT_CLUSTER_NAME = "*";

    @Autowired
    private SecurityService securityService;

    @Autowired
    private List<KHandler> resourceHandlers;

    public V1APIResource findResource(K8s.RESOURCE resourceType, CoreV1Api coreApi) {
        return findResource(resourceType, coreApi, null);
    }

    public V1APIResource findResource(K8s.RESOURCE resourceType, CoreV1Api coreApi, Principal principal) {
        if (principal == null)
            principal = securityService.getPrincipal();
        final var principalFinal = principal;

        var types = K8s.getResourceTypes(coreApi);
        var resType = K8s.findResource(resourceType, types);

        final var defaultRole = securityService.getRolesForResource(AaaConfiguration.SCOPE_DEFAULT,AaaConfiguration.SCOPE_RESOURCE);
        return securityService.hasRole(AaaConfiguration.SCOPE_RESOURCE, resType.getName(), defaultRole, principalFinal ) ? resType : null;

    }

    public CompletableFuture<List<V1APIResource>> getResourceTypesAsync(CoreV1Api coreApi) {
        return getResourceTypesAsync(coreApi, null);
    }

    public CompletableFuture<List<V1APIResource>> getResourceTypesAsync(CoreV1Api coreApi, Principal principal) {
        if (principal == null)
            principal = securityService.getPrincipal();
        final var principalFinal = principal;

        SecurityContext cc = SecurityContext.create(); // need to export the configuration context to another thread

        CompletableFuture<List<V1APIResource>> future = new CompletableFuture<>();
        K8s.getResourceTypesAsync(coreApi).handle((resources, t) -> {
            if (t != null) {
                future.completeExceptionally(t);
                return Collections.emptyList();
            }
            try (SecurityContext.Environment cce = cc.enter()) {
                final var defaultRole = securityService.getRolesForResource(AaaConfiguration.SCOPE_DEFAULT, AaaConfiguration.SCOPE_RESOURCE);
                resources = resources.stream().filter(res -> securityService.hasRole(AaaConfiguration.SCOPE_RESOURCE, res.getName(), defaultRole, principalFinal)).toList();
                future.complete(resources);
                return resources;
            }
        });
        return future;
    }

    public List<V1APIResource> getResourceTypes(CoreV1Api coreApi) {
        return getResourceTypes(coreApi, null);
    }

    public List<V1APIResource> getResourceTypes(CoreV1Api coreApi, Principal principal) {
        if (principal == null)
            principal = securityService.getPrincipal();
        final var principalFinal = principal;

        final var defaultRole = securityService.getRolesForResource(AaaConfiguration.SCOPE_DEFAULT,AaaConfiguration.SCOPE_RESOURCE);
        return K8s.getResourceTypes(coreApi).stream().filter(res -> securityService.hasRole(AaaConfiguration.SCOPE_RESOURCE, res.getName(), defaultRole, principalFinal )).toList();
    }

    public List<String> getNamespaces(boolean includeAllOption, CoreV1Api coreApi) {
        return getNamespaces(includeAllOption, coreApi, null);
    }

    public List<String> getNamespaces(boolean includeAllOption, CoreV1Api coreApi, Principal principal) {
        if (principal == null)
            principal = securityService.getPrincipal();
        final var principalFinal = principal;

        var namespaces = K8s.getNamespaces(coreApi);
        final var defaultRole = securityService.getRolesForResource(AaaConfiguration.SCOPE_DEFAULT,AaaConfiguration.SCOPE_NAMESPACE);
        if (includeAllOption && securityService.hasRole(AaaConfiguration.SCOPE_DEFAULT, AaaConfiguration.SCOPE_NAMESPACE + "_all", defaultRole, principalFinal))
            namespaces.addFirst(K8s.NAMESPACE_ALL);

        return namespaces.stream().filter(ns -> securityService.hasRole(AaaConfiguration.SCOPE_NAMESPACE, ns, defaultRole, principalFinal) ).toList();
    }

    public CompletableFuture<List<String>> getNamespacesAsync(boolean includeAllOption, CoreV1Api coreApi) {
        return getNamespacesAsync(includeAllOption, coreApi, null);
    }

    public CompletableFuture<List<String>> getNamespacesAsync(boolean includeAllOption,CoreV1Api coreApi, Principal principal) {
        if (principal == null)
            principal = securityService.getPrincipal();
        final var principalFinal = principal;
        if (principal == null) {
            throw new RuntimeException("Principal not found");
        }

        SecurityContext cc = SecurityContext.create(); // need to export the configuration context to another thread

        CompletableFuture<List<String>> future = new CompletableFuture<>();
        K8s.getNamespacesAsync(coreApi).handle((namespaces, t) -> {
            if (t != null) {
                future.completeExceptionally(t);
                return Collections.emptyList();
            }
            try (SecurityContext.Environment cce = cc.enter()){
                var defaultRole = securityService.getRolesForResource(AaaConfiguration.SCOPE_DEFAULT, AaaConfiguration.SCOPE_NAMESPACE);
                if (includeAllOption && securityService.hasRole(AaaConfiguration.SCOPE_DEFAULT, AaaConfiguration.SCOPE_NAMESPACE + "_all", defaultRole, principalFinal))
                    namespaces.addFirst(K8s.NAMESPACE_ALL);

                namespaces = namespaces.stream().filter(
                        n -> n.equals(K8s.NAMESPACE_ALL) || securityService.hasRole(AaaConfiguration.SCOPE_NAMESPACE, n, defaultRole, principalFinal)).toList();
                future.complete(namespaces);
                return namespaces;
            }
        });
        return future;
    }

    public Set<String> getAvailableContexts() {
        return getAvailableContexts(null);
    }

    public Set<String> getAvailableContexts(Principal principal) {
        if (principal == null)
            principal = securityService.getPrincipal();
        final var principalFinal = principal;

        final Set<String> availableContexts = new TreeSet<>();
        getKubeConfigs().forEach(config -> config.getContexts().forEach(context -> availableContexts.add( (String)((LinkedHashMap)context).get("name") ) ));

        final File clusterCa = new File(SERVICEACCOUNT_CA_PATH);
        if (clusterCa.exists()) {
            availableContexts.add(LOCAL_CLUSTER_NAME);
        }

        var defaultRole = securityService.getRolesForResource(AaaConfiguration.SCOPE_DEFAULT, AaaConfiguration.SCOPE_CLUSTER);
        return availableContexts.stream().filter(ctx -> securityService.hasRole(AaaConfiguration.SCOPE_CLUSTER, ctx, defaultRole, principalFinal)).collect(Collectors.toSet());
    }

    private List<File> findKubeConfigFiles() {
        List<File> kubeConfigFiles = new LinkedList<>();
        if (System.getenv(ENV_KUBECONFIG) != null) {
            for (String kubeConfigPath : System.getenv(ENV_KUBECONFIG).split(":")) {
                File kubeConfigFile = new File(kubeConfigPath);
                if (kubeConfigFile.exists()) {
                    kubeConfigFiles.add(kubeConfigFile);
                }
            }
        } else {
            File kubeConfigFile = new File(System.getenv("HOME") + "/.kube/config");
            if (kubeConfigFile.exists()) {
                kubeConfigFiles.add(kubeConfigFile);
            }
        }

        return kubeConfigFiles;
    }

    private List<KubeConfig> getKubeConfigs() {

        LinkedList<KubeConfig> kubeConfigs = new LinkedList<>();
        for (File kubeConfigFile : findKubeConfigFiles()) {
            try (final FileReader kubeConfigReader = new FileReader(kubeConfigFile)) {
                KubeConfig kubeConfig = KubeConfig.loadKubeConfig(kubeConfigReader);
                kubeConfigs.add(kubeConfig);
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
            return kubeConfigs;
        }
        return kubeConfigs;
    }


    private KubeConfig getKubeContext(String contextName) {
        for (KubeConfig kubeConfig : getKubeConfigs()) {
            if (kubeConfig.getContexts().stream().anyMatch(context -> contextName.equals(((LinkedHashMap)context).get("name")))) {
                return kubeConfig;
            }
        }

        throw new RuntimeException("Context not found: " + contextName);
    }

    public ApiClient getKubeClient(String contextName) throws IOException {
        if (contextName.equals(DEFAULT_CLUSTER_NAME)) {
            return Config.defaultClient();
        }
        if (contextName.equals(LOCAL_CLUSTER_NAME)) {
            return Config.fromCluster();
        }
        final var kubeConfig = getKubeContext(contextName);
        if (!kubeConfig.setContext(contextName)) {
            throw new RuntimeException("Context not found: " + contextName);
        }
        LOGGER.info("load client for {}: {} {}",contextName, kubeConfig.getCurrentContext(), kubeConfig.getServer());
        return Config.fromConfig(kubeConfig);
    }

    public CoreV1Api getCoreV1Api(String contextName) throws IOException {
        final var kubeClient = getKubeClient(contextName);
        CoreV1Api api = new CoreV1Api(kubeClient);
        return api;
    }

    public KHandler getResourceHandler(String kind) {
        return resourceHandlers.stream().filter(h -> h.getManagedKind().equals(kind)).findFirst().orElse(null);
    }

    public K8s.RESOURCE findResource(V1APIResource value) {
        if (value == null)
            return null;
        if (value.getKind() != null)
            return Arrays.stream(K8s.RESOURCE.values()).filter(r -> r.kind().equalsIgnoreCase(value.getKind())).findFirst()
                    .orElseThrow(() -> new NotFoundRuntimeException("Resource not found: " + value.getName()));
        if (value.getName() != null)
            return Arrays.stream(K8s.RESOURCE.values()).filter(r -> r.resourceType().equalsIgnoreCase(value.getName())).findFirst()
                    .orElseThrow(() -> new NotFoundRuntimeException("Resource not found: " + value.getName()));
        throw new NotFoundRuntimeException("Resource not found: " + value.getName());
    }
}

