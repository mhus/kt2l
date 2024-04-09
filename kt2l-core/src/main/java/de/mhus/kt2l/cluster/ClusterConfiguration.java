package de.mhus.kt2l.cluster;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.ui.UiUtil;

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

    public String defaultResourceType() {
        return config.getString("defaultResourceType", K8sUtil.RESOURCE_PODS);
    }

    public String defaultNamespace() {
        return config.getString("defaultNamespace", K8sUtil.NAMESPACE_ALL);
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
                        cluster.getString("defaultNamespace").orElse(defaultNamespace()),
                        cluster.getString("defaultResourceType").orElse(defaultResourceType()),
                        UiUtil.toColor(cluster.getString("color").orElse(null)),
                        cluster
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
            return new Cluster(name, name, true, defaultNamespace(), defaultResourceType(), UiUtil.COLOR.NONE, MTree.EMPTY_MAP);
        }
        return cluster;
    }

    public static record Cluster(String name, String title, boolean enabled, String defaultNamespace, String defaultResourceType, UiUtil.COLOR color,
                                 ITreeNode node) {}

}
