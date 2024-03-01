package de.mhus.kt2l;

import org.apache.catalina.Realm;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ScheduledExecutorService;

@EnableScheduling
@SpringBootApplication
public class Kt2lApplication {

	public static void main(String[] args) {
		SpringApplication.run(Kt2lApplication.class, args);
	}

	@Bean
	ScheduledExecutorService scheduler() {
		return java.util.concurrent.Executors.newScheduledThreadPool(10);
	}

}
