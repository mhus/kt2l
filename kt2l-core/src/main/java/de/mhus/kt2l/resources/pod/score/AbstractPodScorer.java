package de.mhus.kt2l.resources.pod.score;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class AbstractPodScorer implements PodScorer {

    @Autowired
    private PodScorerConfiguration podScorerConfiguration;
    protected PodScorerConfiguration.Config config;

    @PostConstruct
    public void init() {
        config = podScorerConfiguration.getConfig(getClass());
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }
}
