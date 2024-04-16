package de.mhus.kt2l.config;

import de.mhus.commons.tree.MTree;
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
    public static final String SCOPE_NAMESPACE = "namespace";
    public static final String SCOPE_DEFAULT = "default";
    public static final String SCOPE_CLUSTER = "cluster";
    public static final String SCOPE_CLUSTER_ACTION = "cluster_action";

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
