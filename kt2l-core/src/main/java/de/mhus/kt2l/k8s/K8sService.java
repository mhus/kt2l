package de.mhus.kt2l.k8s;

import de.mhus.kt2l.config.AaaConfiguration;
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

    public V1APIResource findResource(String resourceType, CoreV1Api coreApi) {
        return findResource(resourceType, coreApi, null);
    }

    public V1APIResource findResource(String resourceType, CoreV1Api coreApi, Principal principal) {
        if (principal == null)
            principal = securityService.getPrincipal();
        final var principalFinal = principal;

        var types = K8sUtil.getResourceTypes(coreApi);
        var resType = K8sUtil.findResource(resourceType, types);

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

        CompletableFuture<List<V1APIResource>> future = new CompletableFuture<>();
        K8sUtil.getResourceTypesAsync(coreApi).handle((resources, t) -> {
            if (t != null) {
                future.completeExceptionally(t);
                return Collections.emptyList();
            }
            final var defaultRole = securityService.getRolesForResource(AaaConfiguration.SCOPE_DEFAULT,AaaConfiguration.SCOPE_RESOURCE);
            resources = resources.stream().filter(res -> securityService.hasRole(AaaConfiguration.SCOPE_RESOURCE, res.getName(), defaultRole, principalFinal )).toList();
            future.complete(resources);
            return resources;
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
        return K8sUtil.getResourceTypes(coreApi).stream().filter(res -> securityService.hasRole(AaaConfiguration.SCOPE_RESOURCE, res.getName(), defaultRole, principalFinal )).toList();
    }

    public List<String> getNamespaces(boolean includeAllOption, CoreV1Api coreApi) {
        return getNamespaces(includeAllOption, coreApi, null);
    }

    public List<String> getNamespaces(boolean includeAllOption, CoreV1Api coreApi, Principal principal) {
        if (principal == null)
            principal = securityService.getPrincipal();
        final var principalFinal = principal;

        var namespaces = K8sUtil.getNamespaces(coreApi);
        final var defaultRole = securityService.getRolesForResource(AaaConfiguration.SCOPE_DEFAULT,AaaConfiguration.SCOPE_NAMESPACE);
        if (includeAllOption && securityService.hasRole(AaaConfiguration.SCOPE_DEFAULT, AaaConfiguration.SCOPE_NAMESPACE + "_all", defaultRole, principalFinal))
            namespaces.addFirst(K8sUtil.NAMESPACE_ALL);

        return namespaces.stream().filter(ns -> securityService.hasRole(AaaConfiguration.SCOPE_NAMESPACE, ns, defaultRole, principalFinal) ).toList();
    }

    public CompletableFuture<List<String>> getNamespacesAsync(boolean includeAllOption, CoreV1Api coreApi) {
        return getNamespacesAsync(includeAllOption, coreApi, null);
    }

    public CompletableFuture<List<String>> getNamespacesAsync(boolean includeAllOption,CoreV1Api coreApi, Principal principal) {
        if (principal == null)
            principal = securityService.getPrincipal();
        final var principalFinal = principal;

        CompletableFuture<List<String>> future = new CompletableFuture<>();
        K8sUtil.getNamespacesAsync(coreApi).handle((namespaces, t) -> {
            if (t != null) {
                future.completeExceptionally(t);
                return Collections.emptyList();
            }
            var defaultRole = securityService.getRolesForResource(AaaConfiguration.SCOPE_DEFAULT, AaaConfiguration.SCOPE_NAMESPACE);
            if (includeAllOption && securityService.hasRole(AaaConfiguration.SCOPE_DEFAULT, AaaConfiguration.SCOPE_NAMESPACE + "_all", defaultRole, principalFinal))
                namespaces.addFirst(K8sUtil.NAMESPACE_ALL);

            namespaces = namespaces.stream().filter(
                    n -> n.equals(K8sUtil.NAMESPACE_ALL) || securityService.hasRole(AaaConfiguration.SCOPE_NAMESPACE, n, defaultRole, principalFinal) ).toList();
            future.complete(namespaces);
            return namespaces;
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

}

