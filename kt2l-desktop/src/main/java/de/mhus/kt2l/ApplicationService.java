/*
 * kt2l-desktop - kt2l desktop app
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
package de.mhus.kt2l;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import de.mhus.commons.tools.MDate;
import de.mhus.commons.tools.MLang;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tools.MSystem;
import de.mhus.commons.tree.ITreeNode;
import de.mhus.kt2l.config.Configuration;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;

@Slf4j
@Component
public class ApplicationService {

    @Autowired
    private Configuration configuration;

    @PostConstruct
    private void init() {
        var applicationCfg = configuration.getSection("application");
        initLog(applicationCfg);
        initEnvironment(applicationCfg);
        initPath(applicationCfg);
    }

    private void initEnvironment(ITreeNode applicationCfg) {
        try {
            var maybeEnvironment = applicationCfg.getObject("environment");
            if (maybeEnvironment.isEmpty()) return;
            var current = new HashMap(System.getenv());
            for (var entry : maybeEnvironment.get().entrySet()) {
                LOGGER.debug("Add environment {}", entry);
                current.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            MSystem.setEnv(current);
        } catch (Exception e) {
            LOGGER.error("Can't set environment",e);
        }
        LOGGER.info("Env: {}", System.getenv());
    }

    private void initLog(ITreeNode applicationCfg) {
        if (!applicationCfg.getBoolean("enableDebugLog", false)) return;
        var targetFile = new File(configuration.getGlobalConfigurationDirectory().getParent(), "log/debug_"+ MDate.toIso8601(System.currentTimeMillis())+".log");
        LOGGER.debug("Enable additional debug log {}", targetFile.getAbsolutePath());
        try {
            targetFile.getParentFile().mkdirs();

            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            PatternLayoutEncoder ple = new PatternLayoutEncoder();
            ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
            ple.setContext(lc);
            ple.start();

            var fileAppender = new FileAppender<ILoggingEvent>();
            fileAppender.setFile(targetFile.getAbsolutePath());
            fileAppender.setName("additional");

            fileAppender.setEncoder(ple);
            fileAppender.setContext(lc);
            fileAppender.start();

            ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).addAppender(fileAppender);
            ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.DEBUG);

        } catch (Exception e) {
            LOGGER.error("Can't start additional debug log",e);
        }
    }

    private void initPath(ITreeNode applicationCfg) {
        final var path1 = applicationCfg.getString("path", null);
        if (MString.isSet(path1)) {
            MLang.tryThis(() -> MSystem.setEnv("PATH", path1)).onFailure(e -> LOGGER.warn("Can't set path {}", path1, e));
        }
        var pathAdditional = applicationCfg.getString("pathAdditional", null);
        if (MString.isSet(pathAdditional)) {
            final var path2 = pathAdditional + (MSystem.isWindows() ? ";" : ":") + System.getenv("PATH");
            MLang.tryThis(() -> MSystem.setEnv("PATH", path2)).onFailure(e -> LOGGER.warn("Can't set additional path {}", path2, e));
        }
        LOGGER.info("PATH {}", System.getenv("PATH"));
    }

}
