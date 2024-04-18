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