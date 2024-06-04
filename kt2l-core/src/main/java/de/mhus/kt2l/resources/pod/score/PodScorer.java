package de.mhus.kt2l.resources.pod.score;

import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.resources.pod.PodGrid;

public interface PodScorer {

    int scorePod(ApiProvider apiProvider, PodGrid.Resource pod);

    boolean isEnabled();

}
