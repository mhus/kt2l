package de.mhus.kt2l;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.InputStream;

@Slf4j
public class Kt2lServerApplication extends Kt2lApplication {

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(Kt2lApplication.class);
        builder.headless(true);
        ConfigurableApplicationContext context = builder.run(args);
    }

}
