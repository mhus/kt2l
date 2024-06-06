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

    public boolean isEnabled() {
        return config().getBoolean("enabled", true);
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
