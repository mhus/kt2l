package de.mhus.kt2l;

import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.commons.tree.TreeNode;
import de.mhus.commons.tools.MFile;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class Configuration {

    @Value("${configuration.directory:config}")
    private String configurationDirectory;

    private Map<String, ITreeNode> sections = new HashMap<>();

    public synchronized ITreeNode getSection(String sectionName) {
        if (sections.containsKey(sectionName))
            return sections.get(sectionName);
        final var file = new File(configurationDirectory + "/" + MFile.normalize(sectionName.toLowerCase()) + ".yaml");

        ITreeNode section = null;
        if (file.exists()) {
            section = Try.of(() -> MTree.load(file)).getOrElse(new TreeNode());
        } else {
            LOGGER.info("Configuration {} not found", file.getAbsolutePath());
            section = new TreeNode();
        }
        sections.put(sectionName, section);
        return section;
    }

    public LoginConfiguration getLoginConfiguration() {
        return new LoginConfiguration(getSection("login"));
    }

    public UserDetailsConfiguration getUserDetailsConfiguration() {
        return new UserDetailsConfiguration(getSection("users"));
    }

    public ClusterConfiguration getClusterConfiguration() {
        return new ClusterConfiguration(getSection("clusters"));
    }

}
