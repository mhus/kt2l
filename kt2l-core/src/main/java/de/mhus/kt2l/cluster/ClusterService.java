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
import de.mhus.kt2l.aaa.AaaConfiguration;
import de.mhus.kt2l.aaa.SecurityService;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.CoreAction;
import de.mhus.kt2l.k8s.K8sService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class ClusterService {

    @Autowired
    private ClusterConfiguration clustersConfig;
    @Autowired
    private K8sService k8s;
    @Autowired(required = false)
    private List<CoreAction> coreActions;
    @Autowired(required = false)
    private List<ClusterAction> clusterActions;
    @Autowired
    private SecurityService securityService;

    public Cluster getCluster(String clusterName) {
        if (!isClusterSelectorEnabled() && !clusterName.equals(clustersConfig.defaultClusterName())) {
            throw new NotFoundRuntimeException("Cluster not default: " + clusterName);
        }
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

    public List<Cluster> getAvailableClusters() {
        if (!isClusterSelectorEnabled()) {
            return List.of(getCluster(clustersConfig.defaultClusterName()));
        }
        return k8s.getAvailableContexts().stream()
                .map(name -> getCluster(name))
                .filter(cluster -> cluster.isEnabled())
                .toList();
    }

    public boolean isClusterSelectorEnabled() {
        return clustersConfig.defaultClusterName() == null || clustersConfig.isClusterSelectorEnabled();
    }

    public List<CoreAction> getCoreActions(Core core) {
        if (coreActions == null) return List.of();
        return coreActions.stream()
                .filter(action ->
                        securityService.hasRole(AaaConfiguration.SCOPE_CORE_ACTION, action) && action.canHandle(core))
                .sorted(Comparator.comparingInt(a -> a.getPriority()))
                .toList();
    }

    public List<ClusterAction> getClusterActions(Core core) {
        if (clusterActions == null) return List.of();
        return clusterActions.stream()
                .filter(action -> securityService.hasRole(AaaConfiguration.SCOPE_CLUSTER_ACTION, action) && action.canHandle(core))
                .sorted(Comparator.comparingInt(a -> a.getPriority()))
                .toList();
    }
}
