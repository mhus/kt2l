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

package de.mhus.kt2l.config;

import de.mhus.commons.io.Zip;
import de.mhus.commons.tools.MCollection;
import de.mhus.commons.tools.MFile;
import de.mhus.commons.tools.MSystem;
import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.commons.tree.TreeNode;
import de.mhus.kt2l.generated.DeployInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static de.mhus.commons.tools.MCollection.toSet;
import static de.mhus.commons.tools.MLang.tryThis;
import static de.mhus.commons.tools.MString.isEmpty;
import static de.mhus.commons.tools.MString.isSet;

@Component
@Slf4j
public class Configuration {

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

    @Value("${configuration.tmpDirectory:}")
    private String tmpDirectory;

    private static final Set<String> protectedConfigs = Collections.synchronizedSet(toSet("users", "aaa", "login"));

    private Map<String, ITreeNode> sections = new HashMap<>();
    private File configurationDirectoryFile;
    @Getter
    private File tmpDirectoryFile;

    @PostConstruct
    public void init() {
        LOGGER.info("Application version {} created {}", DeployInfo.VERSION, DeployInfo.CREATED);

        if (configurationDirectory.startsWith("~")) {
            configurationDirectory = System.getProperty("user.home") + configurationDirectory.substring(1);
        }
        LOGGER.info("Configuration directory is {}, recreate: {}", configurationDirectory, createConfiguration);
        if (createConfiguration) {
            initHomeConfiguration();
        }

        if (isEmpty(tmpDirectory)) {
            tmpDirectoryFile = new File(MSystem.getTmpDirectory());
        } else {
            tmpDirectoryFile = new File(tmpDirectory);
        }
        LOGGER.info("Tmp directory is {}", tmpDirectoryFile.getAbsolutePath());

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

        try {
            var errors = Zip.builder().srcStream(zipFile).dst(dir).throwException(false).build().unzip().getErrors();
            if (MCollection.isSet(errors))
                LOGGER.error("Error on unzip: {}", errors);
        } catch (Exception e) {
            LOGGER.error("Can't unzip default configuration package", e);
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
            section = tryThis(() -> MTree.load(finalFile)).onFailure(
                    e -> LOGGER.error("Can't load configuration {} from {}", sectionName, finalFile.getAbsolutePath(), e)
            ).or(new TreeNode());
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

}
