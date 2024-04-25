package de.mhus.kt2l.config;

import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.cluster.ClusterConfiguration;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.stereotype.Component;

@Component
public class ShellConfiguration extends  AbstractUserRelatedConfig {
    protected ShellConfiguration() {
        super("shell");
    }

    public String getShellFor(ClusterConfiguration.Cluster clusterConfig, V1Pod pod) {
        var image = pod.getStatus().getContainerStatuses().get(0).getImage();
        return getShellFor(clusterConfig, pod, image);
    }

    public String getShellFor(ClusterConfiguration.Cluster clusterConfig, V1Pod pod, String containerImage) {
        var config = config();

        if (clusterConfig != null) {
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
        }

        {
            var section = config.getObject("images").orElse(MTree.EMPTY_MAP);
            var entry = section.getString(containerImage);
            if (entry.isPresent()) return entry.get();
        }
        var section = config.getObject("contains").orElse(MTree.EMPTY_MAP);
        for (String key : config.getPropertyKeys()) {
            if (containerImage.contains(key)) {
                var entry = config.getString(key);
                if (entry.isPresent()) return entry.get();
            }
        }
        return config.getString("default").orElse("/bin/sh");
    }

    public String getShell() {
        return config().getString("shell", "/bin/sh");
    }
}
