package de.mhus.kt2l;

import com.vaadin.flow.component.UI;
import de.mhus.commons.tools.MCollection;
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

    public static final String SECTION_VIEWS = "views";
    public static final String SECTION_CLUSTERS = "clusters";
    public static final String SECTION_USERS = "users";
    public static final String SECTION_LOGIN = "login";

    @Value("${configuration.directory:config}")
    private String configurationDirectory;

    @Value("${users.directory:config/users}")
    private String usersDirectory;

    private static final String[] PROTECTED_CONFIGS = {"users"};

    private Map<String, ITreeNode> sections = new HashMap<>();

    public synchronized ITreeNode getSection(String sectionNameIn) {
        if (sections.containsKey(sectionNameIn))
            return sections.get(sectionNameIn);

        final var userName = Try.of(() -> (String)UI.getCurrent().getSession().getAttribute(Kt2lApplication.UI_USERNAME)).getOrElse((String)null);
        final var sectionName = sectionNameIn.toLowerCase();

        File file = null;
        if (!MCollection.contains(PROTECTED_CONFIGS, sectionName) && userName != null) {
            final var user = MFile.normalize(userName.toLowerCase());
            file = new File(usersDirectory + "/" + user + "/" + MFile.normalize(sectionName) + ".yaml");
        }
        if (file == null || !file.exists())
            file = new File(configurationDirectory + "/" + MFile.normalize(sectionName) + ".yaml");
        final var finalFile = file;

        ITreeNode section = null;
        if (finalFile.exists()) {
            LOGGER.info("Load configuration {} from {}", sectionName, file.getAbsolutePath());
            section = Try.of(() -> MTree.load(finalFile)).onFailure(
                    e -> LOGGER.error("Can't load configuration {} from {}", sectionName, finalFile.getAbsolutePath(), e)
            ).getOrElse(new TreeNode());
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
