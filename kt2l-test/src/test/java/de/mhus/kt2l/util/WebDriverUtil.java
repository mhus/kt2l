package de.mhus.kt2l.util;

import de.mhus.commons.tools.MLang;
import de.mhus.kt2l.DebugTestUtil;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

@Slf4j
public class WebDriverUtil {

    public static WebDriver open() {

        LOGGER.info("Open Browser");
        WebDriverManager wdm = WebDriverManager.firefoxdriver();
//        wdm.clearDriverCache().setup();
//        wdm.clearResolutionCache().setup();
        wdm.setup();

        FirefoxOptions browserOptions = new FirefoxOptions();
        browserOptions.addArguments("--no-sandbox");
        if (!DebugTestUtil.TEST_HEADLESS)
            browserOptions.addArguments("--headless");
        browserOptions.addArguments("disable-gpu");

        var browser = new FirefoxDriver(browserOptions);
        browser.manage().window().setSize(new org.openqa.selenium.Dimension(1920, 1080));
        return browser;
    }

    public static void close(WebDriver driver) {
        LOGGER.info("Close Browser");
        MLang.tryThis(() -> driver.quit()).onFailure(e -> LOGGER.error("Error on quit", e));
    }
}
