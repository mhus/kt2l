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
package de.mhus.kt2l.config;

import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.cluster.Cluster;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.stereotype.Component;

@Component
public class ShellConfiguration extends  AbstractUserRelatedConfig {
    protected ShellConfiguration() {
        super("shell");
    }

    public String getShellFor(Cluster clusterConfig, V1Pod pod) {
        var image = pod.getStatus().getContainerStatuses().get(0).getImage();
        return getShellFor(clusterConfig, pod, image);
    }

    public String getShellFor(Cluster clusterConfig, V1Pod pod, String containerImage) {
        var config = config();

        if (clusterConfig != null) {
            var clusterShellConfig = clusterConfig.getNode().getObject("shell");
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
