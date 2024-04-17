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
package de.mhus.kt2l.config;

import com.vaadin.flow.component.UI;
import de.mhus.commons.errors.AuthorizationException;
import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.util.SoftHashMap;
import de.mhus.kt2l.Kt2lApplication;
import io.vavr.control.Try;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

@Slf4j
public abstract class AbstractUserRelatedConfig {

    @Getter
    private final String sectionName;
    private final boolean protectedConfig;

    @Autowired
    private Configuration configuration;

    private SoftHashMap<String, ITreeNode> cache = new SoftHashMap<>();

    protected AbstractUserRelatedConfig(String sectionName) {
        this(sectionName, false);
    }

    @PostConstruct
    public void init() {
        if (protectedConfig) {
            configuration.addProtectedConfiguration(sectionName);
        }
    }

    protected AbstractUserRelatedConfig(String sectionName, boolean isProtected) {
        this.sectionName = sectionName;
        this.protectedConfig = isProtected;
    }

    protected ITreeNode config() {
        final var userName = Configuration.lookupUserName();
        synchronized (cache) {
            var config = cache.get(userName);
            if (config == null) {
                config = loadConfig(userName);
                cache.put(userName, config);
            }
            return config;
        }
    }

    protected void clearCache() {
        synchronized (cache) {
            cache.clear();
        }
    }

    protected void clearCache(String userName) {
        synchronized (cache) {
            cache.remove(userName);
        }
    }

    private ITreeNode loadConfig(String userName) {
        return configuration.getSection(sectionName, userName);
    }

}
