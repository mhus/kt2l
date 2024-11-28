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
package de.mhus.kt2l.resources.pod.score;

import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.resources.pod.PodGrid;
import org.springframework.stereotype.Component;

@Component
public class Metrics extends AbstractPodScorer {

    @Override
    public int scorePod(Cluster cluster, ApiProvider apiProvider, PodGrid.Resource pod) {
        if (!cluster.isMetricsEnabled())
            return 0;
        int points = 0;
        var memoryThreshold = config.getConfig().getInt("memory", 0);
        if (memoryThreshold > 0 && pod.getMetricMemoryPercentage() > memoryThreshold) {
            points+= config.getSpread() + (pod.getMetricMemoryPercentage() - memoryThreshold) * config.getConfig().getInt("memorySpread", 1);
        }
        var cpuThreshold = config.getConfig().getInt("cpu", 0);
        if (cpuThreshold > 0 && pod.getMetricCpuPercentage() > cpuThreshold) {
            points+= config.getSpread() + (pod.getMetricCpuPercentage() - cpuThreshold) * config.getConfig().getInt("cpuSpread", 1);
        }
        return points;
    }
}
