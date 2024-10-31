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
package de.mhus.kt2l.system;

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
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
public class SystemService {

    @Autowired
    private Configuration configuration;

    private long startTime = System.currentTimeMillis();

    @PostConstruct
    private void init() {
        var systemCfg = configuration.getSection("system");
        initPath(systemCfg);
        initLog(systemCfg);
    }

    private void initLog(ITreeNode systemCfg) {
        if (!systemCfg.getBoolean("enableDebugLog", false)) return;
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

    private void initPath(ITreeNode systemCfg) {
        final var path1 = systemCfg.getString("path", null);
        if (MString.isSet(path1)) {
            MLang.tryThis(() -> MSystem.setEnv("PATH", path1)).onFailure(e -> LOGGER.warn("Can't set path {}", path1, e));
        }
        var pathAdditional = systemCfg.getString("pathAdditional", null);
        if (MString.isSet(pathAdditional)) {
            final var path2 = pathAdditional + (MSystem.isWindows() ? ";" : ":") + System.getenv("PATH");
            MLang.tryThis(() -> MSystem.setEnv("PATH", path2)).onFailure(e -> LOGGER.warn("Can't set additional path {}", path2, e));
        }
        LOGGER.info("PATH {}", System.getenv("PATH"));
    }


    public long getUpTime() {
        return System.currentTimeMillis() - startTime;
    }

    public String getUpTimeFormatted() {
        long time = getUpTime();
        long sec = time / 1000;
        long min = sec / 60;
        long hour = min / 60;
        long day = hour / 24;
        return String.format("%d days %02d:%02d:%02d", day, hour % 24, min % 60, sec % 60);
    }

}
