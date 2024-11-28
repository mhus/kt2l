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

package de.mhus.kt2l.aaa;

import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.config.AbstractUserRelatedConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AaaConfiguration extends AbstractUserRelatedConfig {

    public static final String SCOPE_RESOURCE_ACTION = "resource_action";
    public static final String SCOPE_RESOURCE_GRID = "resource_grid";
    public static final String SCOPE_RESOURCE = "resource";
    public static final String SCOPE_RESOURCE_DELETE = "resource_delete";
    public static final String SCOPE_NAMESPACE = "namespace";
    public static final String SCOPE_DEFAULT = "default";
    public static final String SCOPE_CLUSTER = "cluster";
    public static final String SCOPE_CLUSTER_ACTION = "cluster_action";
    public static final String SCOPE_CORE = "core";
    public static final String SCOPE_CORE_ACTION = "core_action";
    public static final String SCOPE_CFG = "cfg";

    public AaaConfiguration() {
        super("aaa", true);
    }

    public Set<String> getRoles(String resourceScope, String resourceName) {
        final var values = config()
                .getObject(resourceScope).orElse(MTree.EMPTY_MAP)
                .getString(resourceName, null);
        if (values == null) return null;
        try {
            return Collections.unmodifiableSet(Arrays.stream(values.split(",")).map(v -> v.trim().toUpperCase()).collect(Collectors.toSet()) );
        } catch (Exception e) {
            LOGGER.error("Can't create ROLE array for {} with {}", resourceName, values, e);
        }
        return null;
    }
}
