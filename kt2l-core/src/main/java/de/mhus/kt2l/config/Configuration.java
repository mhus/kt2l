package de.mhus.kt2l.config;

import com.vaadin.flow.component.UI;
import de.mhus.commons.tools.MFile;
import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.commons.tree.TreeNode;
import de.mhus.kt2l.Kt2lApplication;
import de.mhus.kt2l.ai.AiConfiguration;
import de.mhus.kt2l.cluster.ClusterConfiguration;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static de.mhus.commons.tools.MString.isSet;

@Component
@Slf4j
public class Configuration {

    public static final String SECTION_VIEWS = "views";
    public static final String SECTION_CLUSTERS = "clusters";
    public static final String SECTION_USERS = "users";
    public static final String SECTION_LOGIN = "login";
    private static final String LOCAL_DIR = "local";
    private static final String USERS_DIR = "users";

    @Value("${configuration.directory:config}")
    private String configurationDirectory;

    @Value("${configuration.usersDirectory:}")
    private String usersDirectory;

    @Value("${configuration.localDirectory:}")
    private String localDirectory;

    @Value("${configuration.create:false}")
    private boolean createConfiguration;

    private static final Set<String> PROTECTED_CONFIGS = Collections.unmodifiableSet(Set.of("users", "aaa", "login"));

    private Map<String, ITreeNode> sections = new HashMap<>();
    private File configurationDirectoryFile;

    @PostConstruct
    public void init() {

        if (configurationDirectory.startsWith("~")) {
            configurationDirectory = System.getProperty("user.home") + configurationDirectory.substring(1);
        }
        if (createConfiguration) {
            initHomeConfiguration();
        }

        configurationDirectoryFile = new File(configurationDirectory);
        LOGGER.info("Configuration initialized on {}", configurationDirectoryFile.getAbsolutePath());
    }

    private void initHomeConfiguration() {
        var dir = new File(configurationDirectory);
        // if (dir.exists()) return;
        dir.mkdirs();
        var zipFile = getClass().getResourceAsStream("/config.zip");
        if (zipFile == null) {
            LOGGER.error("Can't find default configuration package");
            return;
        }
        unzip(zipFile, dir);

    }

    private static void unzip(InputStream fis, File dir) {
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        try {
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while(ze != null) {
                if (ze.isDirectory()) {
                    String fileName = ze.getName();
                    File newFile = new File(dir, fileName);
                    newFile.mkdirs();
                } else {
                    String fileName = ze.getName();
                    File newFile = new File(dir, fileName);
                    LOGGER.debug("Unzipping to {}", newFile.getAbsolutePath());
                    //create directories for sub directories in zip
                    newFile.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    //close this ZipEntry
                    zis.closeEntry();
                }
                ze = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
        } catch (IOException e) {
            LOGGER.error("Can't unzip", e);
        }

    }

    public synchronized ITreeNode getSection(String sectionNameIn) {
        if (sections.containsKey(sectionNameIn))
            return sections.get(sectionNameIn);

        final var userName = Try.of(() -> (String)UI.getCurrent().getSession().getAttribute(Kt2lApplication.UI_USERNAME)).getOrElse((String)null);
        final var sectionName = sectionNameIn.toLowerCase();

        File file = null;
        final var normalizedSectionFileName = MFile.normalize(sectionName) + ".yaml";
        if (!PROTECTED_CONFIGS.contains(sectionName) && userName != null) {
            final var user = MFile.normalize(userName.toLowerCase());
            file = new File(getUserConfigurationDirectory(user), normalizedSectionFileName);
        }
        if (file == null || !file.exists())
            file = new File(getLocalConfigurationDirectory(), normalizedSectionFileName);
        if (file == null || !file.exists())
            file = new File(configurationDirectoryFile, normalizedSectionFileName);
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

    private File getLocalConfigurationDirectory() {
        if (isSet(localDirectory))
            return new File(localDirectory);
        return new File( configurationDirectoryFile, LOCAL_DIR);
    }

    private File getUserConfigurationDirectory(String user) {
        if (isSet(usersDirectory))
            return new File(usersDirectory + "/" + user);
        return new File( configurationDirectoryFile, USERS_DIR + "/" + user);
    }

    public UsersConfiguration getUserDetailsConfiguration() {
        return new UsersConfiguration(getSection("users"));
    }

    public ClusterConfiguration getClusterConfiguration() {
        return new ClusterConfiguration(getSection("clusters"));
    }

    public AiConfiguration getAiConfiguration() {
        return new AiConfiguration(getSection("ai"));
    }
}
