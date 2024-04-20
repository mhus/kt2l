package de.mhus.kt2l;

import de.mhus.commons.tools.MLang;
import de.mhus.commons.tools.MThread;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Import;

import java.io.IOException;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        driver.get("http://localhost:"+webServerAppCtxt.getWebServer().getPort());
        MThread.sleep(1000 * 2);
        driver.get("http://localhost:"+webServerAppCtxt.getWebServer().getPort());
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(titleIs("KT2L"));
    }

        @Test
    @Order(2)
    public void testLogin() {
        resetUi();
        DebugTestUtil.debugBreakpoint("After Login");
    }

}
