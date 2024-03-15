package de.mhus.kt2l.config;

import de.mhus.kt2l.cluster.ClusterConfiguration;
import io.kubernetes.client.openapi.models.V1Pod;

public class ConfigUtil {
    public static String getShellFor(Configuration configuration, ClusterConfiguration.Cluster clusterConfig, V1Pod pod) {
        var image = pod.getStatus().getContainerStatuses().get(0).getImage();
        return getShellFor(configuration, clusterConfig, pod, image);
    }

    public static String getShellFor(Configuration configuration, ClusterConfiguration.Cluster clusterConfig, V1Pod pod, String containerImage) {
        var config = configuration.getSection("shell");

        var clusterShellConfig = clusterConfig.node().getObject("shell");
        if (clusterShellConfig.isPresent()) {
            {
                var entry = clusterShellConfig.get().getString(pod.getMetadata().getNamespace() + "." + pod.getMetadata().getName());
                if (entry.isPresent()) return entry.get();
            }
            {
                var entry = clusterShellConfig.get().getString(pod.getMetadata().getName());
                if (entry.isPresent()) return entry.get();
            }
        }

        {
            var entry = config.getString(containerImage);
            if (entry.isPresent()) return entry.get();
        }
        for (String key : config.getPropertyKeys()) {
            if (containerImage.contains(key)) {
                var entry = config.getString(key);
                if (entry.isPresent()) return entry.get();
            }
        }
        return config.getString("default").orElse("/bin/sh");
    }

}