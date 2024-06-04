package de.mhus.kt2l.resources.pod.score;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.config.AbstractUserRelatedConfig;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
public class PodScorerConfiguration extends AbstractUserRelatedConfig {

    protected PodScorerConfiguration() {
        super("pod_scorer");
    }

    public Config getConfig(Class<? extends PodScorer> scorerClass) {
        return new Config(this, scorerClass.getSimpleName().toLowerCase());
    }

    public int getErrorThreshold() {
        return config().getObject("alerts").orElse(MTree.EMPTY_MAP).getInt("error", 1000);
    }

    public int getWarnThreshold() {
        return config().getObject("alerts").orElse(MTree.EMPTY_MAP).getInt("warn", 1000);
    }

    public static class Config {

        private final PodScorerConfiguration podScorerConfiguration;
        @Getter
        private final String name;

        public Config(PodScorerConfiguration podScorerConfiguration, String name) {
            this.podScorerConfiguration = podScorerConfiguration;
            this.name = name;
        }

        public boolean isEnabled() {
            return getConfig().getBoolean("enabled", true);
        }

        public ITreeNode getConfig() {
            return podScorerConfiguration.config().getObject(name).orElse(MTree.EMPTY_MAP);
        }

        public int getSpread() {
            return getConfig().getInt("spread", 1);
        }

        public long getAge() {
            return getConfig().getInt("age", 0);
        }
    }
}
