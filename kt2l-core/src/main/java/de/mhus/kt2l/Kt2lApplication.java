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

package de.mhus.kt2l;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {"de.mhus.kt2l"})
@Slf4j
@Push(PushMode.AUTOMATIC)
public class Kt2lApplication implements AppShellConfigurator {

	public static void main(String[] args) {
		SpringApplication.run(Kt2lApplication.class, args);
	}

	public static boolean canRestart() {
		return "true".equals(System.getenv("KT2L_RESTART_POSSIBLE"));
	}

	public static void restart() {
		LOGGER.info("Restarting application");
		System.exit(101); // tell the script to restart the application
	}

	@Bean
	ScheduledExecutorService scheduler() {
		return java.util.concurrent.Executors.newScheduledThreadPool(10);
	}

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {
			String[] beanNames = ctx.getBeanDefinitionNames();
			Arrays.sort(beanNames);
			for (String beanName : beanNames) {
				LOGGER.debug("- Bean loaded: {}",beanName);
			}
		};
	}

}
