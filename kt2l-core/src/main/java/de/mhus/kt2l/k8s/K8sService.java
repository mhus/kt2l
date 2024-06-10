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
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.core.SecurityContext;
import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.resources.generic.GenericK8s;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.util.KubeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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
    private List<HandlerK8s> resourceHandlers;

    @Autowired
    private Configuration configuration;

    public V1APIResource findResource(K8s resourceType, ApiProvider apiProvider) {
        return findResource(resourceType, apiProvider, null);
    }

    public V1APIResource findResource(K8s resourceType, ApiProvider apiProvider, Principal principal) {
        if (principal == null)
            principal = securityService.getPrincipal();
        final var principalFinal = principal;

        var types = K8sUtil.getResourceTypes(apiProvider.getCoreV1Api());
        var resType = K8s.toResource(resourceType);

        final var defaultRole = securityService.getRolesForResource(AaaConfiguration.SCOPE_DEFAULT,AaaConfiguration.SCOPE_RESOURCE);
        return securityService.hasRole(AaaConfiguration.SCOPE_RESOURCE, resType.getName(), defaultRole, principalFinal ) ? resType : null;

    }

    public CompletableFuture<List<V1APIResource>> getResourceTypesAsync(ApiProvider apiProvider) {
        return getResourceTypesAsync(apiProvider, null);
    }

    public CompletableFuture<List<V1APIResource>> getResourceTypesAsync(ApiProvider apiProvider, Principal principal) {
        if (principal == null)
            principal = securityService.getPrincipal();
        final var principalFinal = principal;

        SecurityContext cc = SecurityContext.create(); // need to export the configuration context to another thread

        CompletableFuture<List<V1APIResource>> future = new CompletableFuture<>();
        K8sUtil.getResourceTypesAsync(apiProvider.getCoreV1Api()).handle((resources, t) -> {
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

    public List<V1APIResource> getResourceTypes(ApiProvider apiProvider) {
        return getResourceTypes(apiProvider, null);
    }

    public List<V1APIResource> getResourceTypes(ApiProvider apiProvider, Principal principal) {
        if (principal == null)
            principal = securityService.getPrincipal();
        final var principalFinal = principal;

        final var defaultRole = securityService.getRolesForResource(AaaConfiguration.SCOPE_DEFAULT,AaaConfiguration.SCOPE_RESOURCE);
        return K8sUtil.getResourceTypes(apiProvider.getCoreV1Api()).stream().filter(res -> securityService.hasRole(AaaConfiguration.SCOPE_RESOURCE, res.getName(), defaultRole, principalFinal )).toList();
    }

    public List<String> getNamespaces(boolean includeAllOption, ApiProvider apiProvider) {
        return getNamespaces(includeAllOption, apiProvider, null);
    }

    public List<String> getNamespaces(boolean includeAllOption, ApiProvider apiProvider, Principal principal) {
        if (principal == null)
            principal = securityService.getPrincipal();
        final var principalFinal = principal;

        var namespaces = K8sUtil.getNamespaces(apiProvider.getCoreV1Api());
        final var defaultRole = securityService.getRolesForResource(AaaConfiguration.SCOPE_DEFAULT,AaaConfiguration.SCOPE_NAMESPACE);
        if (includeAllOption && securityService.hasRole(AaaConfiguration.SCOPE_DEFAULT, AaaConfiguration.SCOPE_NAMESPACE + "_all", defaultRole, principalFinal))
            namespaces.addFirst(K8sUtil.NAMESPACE_ALL_LABEL);

        return namespaces.stream().filter(ns -> securityService.hasRole(AaaConfiguration.SCOPE_NAMESPACE, ns, defaultRole, principalFinal) ).toList();
    }

    public CompletableFuture<List<String>> getNamespacesAsync(boolean includeAllOption, ApiProvider apiProvider) {
        return getNamespacesAsync(includeAllOption, apiProvider, null);
    }

    public CompletableFuture<List<String>> getNamespacesAsync(boolean includeAllOption,ApiProvider apiProvider, Principal principal) {
        if (principal == null)
            principal = securityService.getPrincipal();
        final var principalFinal = principal;
        if (principal == null) {
            throw new RuntimeException("Principal not found");
        }

        SecurityContext cc = SecurityContext.create(); // need to export the configuration context to another thread

        CompletableFuture<List<String>> future = new CompletableFuture<>();
        K8sUtil.getNamespacesAsync(apiProvider.getCoreV1Api()).handle((namespaces, t) -> {
            if (t != null) {
                future.completeExceptionally(t);
                return Collections.emptyList();
            }
            try (SecurityContext.Environment cce = cc.enter()){
                var defaultRole = securityService.getRolesForResource(AaaConfiguration.SCOPE_DEFAULT, AaaConfiguration.SCOPE_NAMESPACE);
                if (includeAllOption && securityService.hasRole(AaaConfiguration.SCOPE_DEFAULT, AaaConfiguration.SCOPE_NAMESPACE + "_all", defaultRole, principalFinal))
                    namespaces.addFirst(K8sUtil.NAMESPACE_ALL_LABEL);

                namespaces = namespaces.stream().filter(
                        n -> n.equals(K8sUtil.NAMESPACE_ALL_LABEL) || securityService.hasRole(AaaConfiguration.SCOPE_NAMESPACE, n, defaultRole, principalFinal)).toList();
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
        LOGGER.debug("Available contexts: {}", availableContexts);

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
    public ApiProvider getKubeClient(String contextName) throws IOException {
        return getKubeClient(contextName, ApiProvider.DEFAULT_TIMEOUT);
    }

    public ApiProvider getKubeClient(String contextName, long timeout) throws IOException {
        if (contextName.equals(DEFAULT_CLUSTER_NAME)) {
            return new DefaultClientApiClientProvider(timeout);
        }
        if (contextName.equals(LOCAL_CLUSTER_NAME)) {
            return new FromClusterApiClientProvider(timeout);
        }
        final var kubeConfig = getKubeContext(contextName);
        if (!kubeConfig.setContext(contextName)) {
            throw new RuntimeException("Context not found: " + contextName);
        }
        LOGGER.info("load client for {}: {} {}",contextName, kubeConfig.getCurrentContext(), kubeConfig.getServer());
        return new ClusterApiClientProvider(kubeConfig, timeout);
    }

    public HandlerK8s getResourceHandler(V1APIResource resource) {
        return resourceHandlers.stream().filter(h -> h.getManagedResourceType().kind().equals(resource.getKind())).findFirst().orElseGet(() -> new GenericK8s(resource));
    }

    public HandlerK8s getResourceHandler(K8s resource) {
        return resourceHandlers.stream().filter(h -> h.getManagedResourceType().equals(resource)).findFirst().orElseGet(() -> new GenericK8s(resource));
    }

    public K8s findResource(V1APIResource value) {
        if (value == null)
            return null;
        if (value.getKind() != null)
            return Arrays.stream(K8s.values()).filter(r -> r.kind().equalsIgnoreCase(value.getKind())).findFirst()
                    .orElseThrow(() -> new NotFoundRuntimeException("Resource not found: " + value.getName()));
        if (value.getName() != null)
            return Arrays.stream(K8s.values()).filter(r -> r.resourceType().equalsIgnoreCase(value.getName())).findFirst()
                    .orElseThrow(() -> new NotFoundRuntimeException("Resource not found: " + value.getName()));
        throw new NotFoundRuntimeException("Resource not found: " + value.getName());
    }

//    public Path getKubeConfigPath(Cluster cluster) {
//        if (cluster == null)
//            return null;
//        try {
//            var tmp = new File(configuration.getTmpDirectoryFile(), "kubeconf-" + MFile.normalize(cluster.getName()) + ".conf");
//            if (!tmp.exists() || tmp.lastModified() < System.currentTimeMillis() + cluster.getApiProviderTimeout()) {
//                // create
//                var kubeContext = getKubeContext(cluster.getName());
//                kubeContext.setContext(cluster.getName());
//                Map<String, Object> ctx = K8sUtil.findObject(kubeContext.getContexts(), cluster.getName());
//                String myCluster = (String)ctx.get("cluster");
//                String myUser = (String)ctx.get("user");
//                String currentNamespace = (String)ctx.get("namespace");
//                Map<String, Object> kubeCluster = K8sUtil.findObject(kubeContext.getClusters(), myCluster);
//                Map<String, Object> kubeUser = K8sUtil.findObject(kubeContext.getUsers(), myUser);
//
//                Map<String, Object> configMap = new LinkedHashMap<>();
//                configMap.put("current-context", cluster.getName());
//                configMap.put("contexts", ctx);
//                configMap.put("clusters", kubeCluster);
//                configMap.put("users", kubeUser);
//                var kubeContextStr = Yaml.dump(configMap);
//                MFile.writeFile(tmp, kubeContextStr);
//            }
//            return tmp.toPath();
//        } catch (Exception e) {
//            LOGGER.error("Can't create kube config", e);
//            return null;
//        }
//    }

}

