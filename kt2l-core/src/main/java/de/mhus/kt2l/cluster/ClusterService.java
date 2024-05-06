package de.mhus.kt2l.cluster;

import de.mhus.commons.errors.NotFoundRuntimeException;
import de.mhus.kt2l.k8s.K8sService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class ClusterService {

    @Autowired
    private ClusterConfiguration clustersConfig;

    @Autowired
    private K8sService k8s;


    public Cluster getCluster(String clusterName) {
        var contexts = k8s.getAvailableContexts();
        if (!contexts.contains(clusterName)) {
            throw new NotFoundRuntimeException("Cluster not found: " + clusterName);
        }
        var cluster = clustersConfig.getClusterOrDefault(clusterName);
        cluster.setK8sService(k8s);
        return cluster;
    }

    public Optional<String> defaultClusterName() {
        return Optional.ofNullable(clustersConfig.defaultClusterName());
    }
}
