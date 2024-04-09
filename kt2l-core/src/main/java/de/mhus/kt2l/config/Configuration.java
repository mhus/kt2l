package de.mhus.kt2l.config;

import com.vaadin.flow.component.UI;
import de.mhus.commons.tools.MCollection;
import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.commons.tree.TreeNode;
import de.mhus.commons.tools.MFile;
import de.mhus.kt2l.Kt2lApplication;
import de.mhus.kt2l.ai.AiConfiguration;
import de.mhus.kt2l.cluster.ClusterConfiguration;
import de.mhus.kt2l.ui.LoginConfiguration;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@Slf4j
public class Configuration {

    public static final String SECTION_VIEWS = "views";
    public static final String SECTION_CLUSTERS = "clusters";
    public static final String SECTION_USERS = "users";
    public static final String SECTION_LOGIN = "login";

    @Value("${configuration.directory:config}")
    private String configurationDirectory;

    private static final String[] PROTECTED_CONFIGS = {"users"};

    private Map<String, ITreeNode> sections = new HashMap<>();
    private File configurationDirectoryFile;

    @PostConstruct
    public void init() {

        if (configurationDirectory.startsWith("~")) {
            configurationDirectory = System.getProperty("user.home") + configurationDirectory.substring(1);
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
                String fileName = ze.getName();
                File newFile = new File(dir,  fileName);
                LOGGER.debug("Unzipping to {}", newFile.getAbsolutePath());
                //create directories for sub directories in zip
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                //close this ZipEntry
                zis.closeEntry();
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
        if (!MCollection.contains(PROTECTED_CONFIGS, sectionName) && userName != null) {
            final var user = MFile.normalize(userName.toLowerCase());
            file = new File( configurationDirectoryFile, "users/" + user + "/" + MFile.normalize(sectionName) + ".yaml");
        }
        if (file == null || !file.exists())
            file = new File(configurationDirectoryFile, MFile.normalize(sectionName) + ".yaml");
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

    public AiConfiguration getAiConfiguration() {
        return new AiConfiguration(getSection("ai"));
    }
}
