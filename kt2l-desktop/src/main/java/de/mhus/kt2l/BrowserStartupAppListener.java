package de.mhus.kt2l;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Duration;

@Slf4j
public class BrowserStartupAppListener implements SpringApplicationRunListener {

    @Override
    public void started(ConfigurableApplicationContext context, Duration timeTaken) {
        LOGGER.info("Application started");
        try {
            Thread.sleep(3000);
            var browser = context.getBean(BrowserBean.class);
            browser.loadLocalURL();
        } catch (InterruptedException e) {
            LOGGER.error("Error starting cef browser", e);
        }
    }

}
