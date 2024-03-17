package de.mhus.kt2l;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import de.mhus.commons.tools.MThread;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ScheduledExecutorService;

@EnableScheduling
@SpringBootApplication
public class Kt2lApplication {

	public static final String UI_USERNAME = "username";

	public static void main(String[] args) {
		SpringApplication.run(Kt2lApplication.class, args);
	}

	@Bean
	ScheduledExecutorService scheduler() {
		return java.util.concurrent.Executors.newScheduledThreadPool(10);
	}

}
