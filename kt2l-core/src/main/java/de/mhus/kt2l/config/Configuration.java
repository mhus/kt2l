package de.mhus.kt2l.config;

import com.vaadin.flow.component.UI;
import de.mhus.commons.errors.AuthorizationException;
import de.mhus.commons.lang.ICloseable;
import de.mhus.commons.tools.MFile;
import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.commons.tree.TreeNode;
import de.mhus.kt2l.Kt2lApplication;
import io.vavr.control.Try;
import lombok.Getter;
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

import static de.mhus.commons.tools.MCollection.toSet;
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

    private static final ThreadLocal<ConfigurationContext> threadLocalConfigurationContext = new ThreadLocal<>();

    @Value("${configuration.directory:config}")
    private String configurationDirectory;

    @Value("${configuration.usersDirectory:}")
    private String usersDirectory;

    @Value("${configuration.localDirectory:}")
    private String localDirectory;

    @Value("${configuration.create:false}")
    private boolean createConfiguration;

    private static final Set<String> protectedConfigs = Collections.synchronizedSet(toSet("users", "aaa", "login"));

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

    /**
     * Return a not user related section.
     * This method is not cached.
     *
     * @param sectionNameIn Name of the section
     * @return The section
     */
    public synchronized ITreeNode getSection(String sectionNameIn) {
        return getSection(sectionNameIn, null);
    }

    /**
     * Return a user related section. Or a default section if no user is given.
     * This method is not cached.
     *
     * @param sectionNameIn Name of the section
     * @param userName Name of the user
     * @return The section
     */
    public synchronized ITreeNode getSection(String sectionNameIn, String userName) {
        if (sections.containsKey(sectionNameIn))
            return sections.get(sectionNameIn);

        final var sectionName = sectionNameIn.toLowerCase();
        userName = userName == null ? null : MFile.normalize(userName.toLowerCase());

        File file = null;
        final var normalizedSectionFileName = MFile.normalize(sectionName) + ".yaml";
        // 1. try to get config from user directory
        if (!protectedConfigs.contains(sectionName) && userName != null) {
            final var user = MFile.normalize(userName.toLowerCase());
            file = new File(getUserConfigurationDirectory(user), normalizedSectionFileName);
        }
        // 2. try to get config from local user directory
        if (file == null || !file.exists())
            file = new File(getLocalConfigurationDirectory(), "users/" + userName + "/" + normalizedSectionFileName);
        // 3. try to get config from local directory
        if (file == null || !file.exists())
            file = new File(getLocalConfigurationDirectory(), normalizedSectionFileName);
        // 4. try to get config from configuration directory
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

    void addProtectedConfiguration(String sectionName) {
        LOGGER.info("Add protected configuration {}", sectionName);
        protectedConfigs.add(sectionName);
    }

    public static ConfigurationContext getContext() {
        return new ConfigurationContext();
    }

    public static String lookupUserName() {
        var context = threadLocalConfigurationContext.get();
        final var userName = context != null ? context.getUserName() : Try.of(() -> (String) UI.getCurrent().getSession().getAttribute(Kt2lApplication.UI_USERNAME)).getOrElseThrow(() -> {
            LOGGER.error("Calling config() without user in UI context", new Exception());
            return new AuthorizationException("No user in UI context");
        });
        return userName;
    }

    public static class ConfigurationContext {
        @Getter
        private final String userName;

        protected ConfigurationContext() {
            var context = threadLocalConfigurationContext.get();
            userName = lookupUserName();
        }
        
        public Environment enter() {
            return new Environment(this);
        }
    }

    public static class Environment implements ICloseable {
        private final ConfigurationContext context;

        private Environment(ConfigurationContext context) {
            this.context = context;
            threadLocalConfigurationContext.set(context);
        }

        @Override
        public void close() {
            threadLocalConfigurationContext.remove();
        }
    }
}