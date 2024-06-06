package de.mhus.kt2l.resources.pod.score;

import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.resources.pod.PodGrid;
import org.springframework.stereotype.Component;

@Component
public class Metrics extends AbstractPodScorer {

    @Override
    public int scorePod(ApiProvider apiProvider, PodGrid.Resource pod) {

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
