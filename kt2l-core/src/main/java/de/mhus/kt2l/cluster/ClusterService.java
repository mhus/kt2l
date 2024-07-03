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
package de.mhus.kt2l.cluster;

import de.mhus.commons.errors.NotFoundRuntimeException;
import de.mhus.kt2l.k8s.K8sService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
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

    public List<ClusterOverviewPanel.ClusterItem> getAvailableClusters() {
        return k8s.getAvailableContexts().stream()
                .map(name -> {
                    final var cluster = getCluster(name);
                    return new ClusterOverviewPanel.ClusterItem(name, cluster.getTitle(), cluster);
                })
                .filter(cluster -> cluster.cluster().isEnabled())
                .toList();

    }
}
