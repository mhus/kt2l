package de.mhus.kt2l.k8s;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static io.kubernetes.client.util.Config.ENV_KUBECONFIG;
import static io.kubernetes.client.util.Config.SERVICEACCOUNT_CA_PATH;

@Slf4j
@Component
public class K8sService {

    public static final String LOCAL_CLUSTER_NAME = ".local-cluster";
    public static final String DEFAULT_CLUSTER_NAME = "*";

    public Set<String> availableContexts() {
        Set<String> availableContexts = new TreeSet<>();
        getKubeConfigs().forEach(config -> config.getContexts().forEach(context -> availableContexts.add( (String)((LinkedHashMap)context).get("name") ) ));

        final File clusterCa = new File(SERVICEACCOUNT_CA_PATH);
        if (clusterCa.exists()) {
            availableContexts.add(LOCAL_CLUSTER_NAME);
        }

        return availableContexts;
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

