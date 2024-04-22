package de.mhus.kt2l;

import de.mhus.commons.tools.MLang;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.util.AremoricaContextConfiguration;
import de.mhus.kt2l.util.AremoricaK8sService;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Import;

import java.io.IOException;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.titleIs;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"configuration.localDirectory=local_config"}
)
@Import(AremoricaContextConfiguration.class)
public class LocalServerTest {

    private static ChromeDriver driver;

    @Autowired
    private ServletWebServerApplicationContext webServerAppCtxt;
    private boolean firstResetUi = true;

    @BeforeAll
    public static void beforeAll() throws IOException, ApiException {
        System.out.println("Before All");
        DebugTestUtil.debugPrepare();
        AremoricaK8sService.start();

//        WebDriverManager.chromedriver().clearDriverCache().setup();
//        WebDriverManager.chromedriver().clearResolutionCache().setup();

        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();

        AremoricaK8sService.createAremorica();

    }

    @AfterAll
    public static void afterAll() {
        System.out.println("After All");

        MLang.tryThis(() -> driver.quit()).onError(e -> LOGGER.error("Error on quit", e));
        AremoricaK8sService.stop();
    }

    public void resetUi() {
        LOGGER.info("Reset test on port {}", webServerAppCtxt.getWebServer().getPort());
        if (firstResetUi) {
            driver.get("http://localhost:" + webServerAppCtxt.getWebServer().getPort());
            MThread.sleep(1000 * 2);
            firstResetUi = false;
        }
        driver.get("http://localhost:" + webServerAppCtxt.getWebServer().getPort());
        MThread.sleep(1000 * 1);
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(titleIs("KT2L"));
    }

    @Test
    @Order(2)
    public void testLogin() {
        resetUi();
        DebugTestUtil.doScreenshot(driver, "login");
        DebugTestUtil.debugBreakpoint("Login");

        // Cluster Name
        var clusterSelector = driver.findElement(By.id("input-vaadin-combo-box-4"));
        assertThat(clusterSelector).isNotNull();
        assertThat(clusterSelector.getAttribute("value")).isEqualTo("Aremorica");
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(titleIs("KT2L"));

        assertThat(driver.findElement(By.xpath("//vaadin-menu-bar-item[contains(.,\"Resources\")]"))).isNotNull();

    }

    @Test
    @Order(3)
    public void testDetails() {
        resetUi();

        // click on Resources on Main
        driver.findElement(By.xpath("//vaadin-menu-bar-item[contains(.,\"Resources\")]")).click();
        // wait for the view menu
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(presenceOfElementLocated(By.xpath("//vaadin-menu-bar-item[contains(.,\"View\")]")));

        // click on pod idefix
        driver.findElement(By.xpath("//vaadin-grid-cell-content[contains(.,\"idefix\")]")).click();
        // should be selected
        assertThat(driver.findElement(By.xpath("//vaadin-grid-cell-content[contains(.,\"idefix\")]/../..")).getAttribute("selected")).isEqualTo("true");

        DebugTestUtil.debugBreakpoint("Details");

    }

}