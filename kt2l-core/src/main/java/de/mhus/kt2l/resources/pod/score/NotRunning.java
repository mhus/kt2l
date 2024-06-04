package de.mhus.kt2l.resources.pod.score;

import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.resources.pod.PodGrid;
import org.springframework.stereotype.Component;

@Component
public class NotRunning extends AbstractPodScorer {
    @Override
    public int scorePod(ApiProvider apiProvider, PodGrid.Resource pod) {
        if (pod.getResource().getMetadata().getCreationTimestamp().toEpochSecond() - System.currentTimeMillis()/1000 > config.getAge()) {
            return 0;
        }
        return pod.getRunningContainersCnt() == 0 ? config.getSpread() : 0;
    }
}
