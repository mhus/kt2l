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
import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.config.AbstractUserRelatedConfig;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.core.UiUtil;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;

@Component
public class ClusterConfiguration extends AbstractUserRelatedConfig {

    public ClusterConfiguration() {

        super("clusters", MCast.toboolean(System.getenv("KT2L_PROTECTED_CLUSTERS_CONFIG"), false));
    }

    public String defaultClusterName() {
        return config().getString("defaultCluster", "");
    }

    public String defaultResourceType() {
        return config().getString("defaultResourceType", K8sUtil.RESOURCE_PODS);
    }

    public String defaultNamespace() {
        return config().getString("defaultNamespace", K8sUtil.NAMESPACE_ALL);
    }

    public Map<String, Cluster> getClusters() {
        final var clusters = new TreeMap<String, Cluster>();
        final var clusterConfig = config().getArray("clusters");
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

    public record Cluster(String name, String title, boolean enabled, String defaultNamespace, String defaultResourceType, UiUtil.COLOR color,
                                 ITreeNode node) {
    }

}
