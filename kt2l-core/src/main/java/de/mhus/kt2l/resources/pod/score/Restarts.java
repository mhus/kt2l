package de.mhus.kt2l.resources.pod.score;

import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.resources.pod.PodGrid;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import org.springframework.stereotype.Component;

@Component
public class Restarts extends AbstractPodScorer {
    @Override
    public int scorePod(ApiProvider apiProvider, PodGrid.Resource pod) {
        return pod.getRestarts() * config.getSpread();
    }
}
