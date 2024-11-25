/*
 * kt2l-native - kt2l as native server
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
/**
 * This file is part of kt2l-server.
 *
 * kt2l-server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * kt2l-server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with kt2l-server.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l;

import de.mhus.commons.directory.ClassLoaderResourceProvider;
import de.mhus.commons.services.DefaultEnvironmentProvider;
import de.mhus.commons.tree.DefaultNodeFactory;
import de.mhus.kt2l.generated.DeployInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.ReflectionUtils;

import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class Kt2lNativeServerApplication {

    public static void main(String[] args) {
        LOGGER.info("Start kt2l-server {} {}", DeployInfo.VERSION, DeployInfo.CREATED_DATETIME);
        SpringApplicationBuilder builder = new SpringApplicationBuilder(Kt2lApplication.class);
        builder.headless(true);
        ConfigurableApplicationContext context = builder.run(args);
    }

}
