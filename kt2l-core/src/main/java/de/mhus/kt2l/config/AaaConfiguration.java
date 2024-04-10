package de.mhus.kt2l.config;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AaaConfiguration {

    @Autowired
    private Configuration configuration;
    private ITreeNode aaa;
    private ITreeNode roles;


    @PostConstruct
    private void init() {
        aaa = configuration.getSection("aaa");
        roles = aaa.getObject("roles").orElse(MTree.EMPTY_MAP);
    }

    public Set<String> getRoles(String resourceName) {
        final var values = roles.getString(resourceName, null);
        if (values == null) return null;
        try {
            return Collections.unmodifiableSet(Arrays.stream(values.split(",")).map(v -> v.trim().toUpperCase()).collect(Collectors.toSet()) );
        } catch (Exception e) {
            LOGGER.error("Can't create ROLE array for {} with {}", resourceName, values, e);
        }
        return null;
    }
}
