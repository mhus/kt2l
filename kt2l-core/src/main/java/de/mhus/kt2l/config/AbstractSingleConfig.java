package de.mhus.kt2l.config;

import de.mhus.commons.tree.ITreeNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

@Slf4j
public abstract class AbstractSingleConfig {

    @Getter
    private final String sectionName;
    private final boolean protectedConfig;
    @Autowired
    private Configuration configuration;

    private ITreeNode cache;

    protected AbstractSingleConfig(String sectionName) {
        this(sectionName, false);
    }

    protected AbstractSingleConfig(String sectionName, boolean isProtected) {
        this.sectionName = sectionName;
        this.protectedConfig = isProtected;
    }

    @PostConstruct
    public void init() {
        if (protectedConfig) {
            configuration.addProtectedConfiguration(sectionName);
        }
    }

    protected ITreeNode config() {
        synchronized (this) {
            if (cache == null) {
                cache = loadConfig();
            }
            return cache;
        }
    }

    private ITreeNode loadConfig() {
        return configuration.getSection(sectionName);
    }

    protected void clearCache() {
        synchronized (this) {
            cache = null;
        }
    }

}
