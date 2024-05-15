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

import de.mhus.commons.tools.MCast;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.config.AbstractUserRelatedConfig;
import de.mhus.kt2l.core.SecurityContext;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.k8s.K8s;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

@Component
public class ClusterConfiguration extends AbstractUserRelatedConfig {

    private final Map<String, ClusterInfo> clusterInfos = Collections.synchronizedMap(new WeakHashMap<>());

    public ClusterConfiguration() {

        super("clusters", MCast.toboolean(System.getenv("KT2L_PROTECTED_CLUSTERS_CONFIG"), false));
    }

    public String defaultClusterName() {
        return config().getString("defaultCluster", "");
    }

    public String defaultResourceType() {
        return config().getString("defaultResourceType", K8s.POD.resourceType());
    }

    public String defaultNamespace() {
        return config().getString("defaultNamespace", K8sUtil.NAMESPACE_ALL);
    }

    private synchronized Map<String, Cluster> getClusters() {

        final var clusterInfo = getClusterInfo();

        if (clusterInfo.clusters != null) return clusterInfo.clusters; //XXX refresh if cluster config changes

        clusterInfo.clusters = Collections.synchronizedMap(new TreeMap<>());
        final var clusterConfig = config().getArray("clusters");
        if (clusterConfig.isPresent())
            clusterConfig.get().forEach(cluster -> {
                clusterInfo.clusters.put(cluster.getString("name").get(), new Cluster(
                        this,
                        SecurityContext.lookupUserName(),
                        cluster.getString("name").get(),
                        cluster
                ));
            });
        return clusterInfo.clusters;
    }

    private ClusterInfo getClusterInfo() {
        return clusterInfos.computeIfAbsent(SecurityContext.lookupUserName(), (n) -> new ClusterInfo(n));
    }

//    Cluster getCluster(String name) {
//        return getClusters().get(name);
//    }

    Cluster getClusterOrDefault(String name) {
        final var cluster = getClusters().get(name);
        if (cluster == null) {
            return getDefault(name);
        }
        return cluster;
    }

    private synchronized Cluster getDefault(String name) {
        var clusterInfo = getClusterInfo();
        return clusterInfo.defaultClusters.computeIfAbsent(name,(n) -> new Cluster(this, SecurityContext.lookupUserName(), n, MTree.EMPTY_MAP));
    }

    private class ClusterInfo {
        private final String name;
        private final Map<String, Cluster> defaultClusters = Collections.synchronizedMap(new HashMap<>());
        private Map<String, Cluster> clusters;

        public ClusterInfo(String name) {
            this.name = name;
        }
    }
}
