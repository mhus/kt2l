package de.mhus.kt2l;

import de.mhus.commons.node.ITreeNode;

import java.util.Map;
import java.util.TreeMap;

public class ClusterConfiguration {
    private final ITreeNode config;

    public ClusterConfiguration(ITreeNode config) {
        this.config = config;
    }

    public String defaultClusterName() {
        return config.getString("defaultCluster", "");
    }

    public Map<String, Cluster> getClusters() {
        final var clusters = new TreeMap<String, Cluster>();
        final var clusterConfig = config.getArray("clusters");
        if (clusterConfig.isPresent())
            clusterConfig.get().forEach(cluster -> {
                clusters.put(cluster.getString("name").get(), new Cluster(
                        cluster.getString("name").get(),
                        cluster.getString("title").orElse(cluster.getString("name").get()),
                        cluster.getBoolean("enabled").orElse(true),
                        cluster.getString("defaultNamespace").orElse("all"),
                        cluster.getString("defaultResourceType").orElse("pod")
                ));
            });
        return clusters;
    }

    public Cluster getCluster(String name) {
        return getClusters().get(name);
    }

    public Cluster getClusterOrDefault(String name) {
        final var cluster = getClusters().get(name);
        if (cluster == null) {
            return new Cluster(name, name, true, "all", "pod");
        }
        return cluster;
    }

    public static record Cluster(String name, String title, boolean enabled, String defaultNamespace, String defaultResourceType) {}
}
