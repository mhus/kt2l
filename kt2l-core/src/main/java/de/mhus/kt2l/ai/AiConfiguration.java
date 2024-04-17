/**
 * This file is part of kt2l-core.
 *
 * kt2l-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * kt2l-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with kt2l-core.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l.ai;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.config.AbstractUserRelatedConfig;
import de.mhus.kt2l.config.Configuration;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Getter
@Component
public class AiConfiguration extends AbstractUserRelatedConfig {

    public AiConfiguration() {
        super("ai");
    }

    public String getOllamaUrl() {
        return config().getString("ollamaUrl").orElseGet(() -> String.format("http://%s:%d", "127.0.0.1", 11434));
    }
    public Optional<String> getTemplate(String name) {
        return config().getObject("prompts").orElseGet(() -> MTree.EMPTY_MAP).getObject(name).orElse(MTree.EMPTY_MAP).getString("template");
    }

    public Optional<String> getModel(String name) {
        return config().getObject("prompts").orElseGet(() -> MTree.EMPTY_MAP).getObject(name).orElse(MTree.EMPTY_MAP).getString("model");
    }

    public boolean isEnabled() {
        return config().getBoolean("enabled", false);
    }

    public String getOpenAiKey() {
        return config().getString("openAiKey").orElse(null);
    }
}
