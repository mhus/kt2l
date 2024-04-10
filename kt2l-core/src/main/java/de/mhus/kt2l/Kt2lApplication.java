package de.mhus.kt2l;

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
public class Kt2lApplication {

	public static final String UI_USERNAME = "username";

	public static void main(String[] args) {
		SpringApplication.run(Kt2lApplication.class, args);
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
				var bean = ctx.getBean(beanName);
				LOGGER.debug("- Bean loaded: ({}) of type ({})",beanName, bean.getClass().getName());
			}
		};
	}

}
