/*
 * kt2l-test - kt2l integration tests
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

import de.mhus.commons.tools.MLang;
import de.mhus.commons.tools.MThread;
import de.mhus.commons.util.Value;
import de.mhus.kt2l.resources.ResourcesGridPanel;
import de.mhus.kt2l.resources.pod.PodGrid;
import de.mhus.kt2l.util.App;
import de.mhus.kt2l.util.AremoricaContextConfiguration;
import de.mhus.kt2l.util.AremoricaK8sService;
import de.mhus.kt2l.util.CoreHelper;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.titleIs;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

@Disabled
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
        "kt2l.configuration.localDirectory=config_timeout",
        "kt2l.configuration.usersDirectory=users_nodirectoryset",
        "kt2l.deskTabPreserveMode=false",
        "server.session.timeout=2"
        }
)
@Import({AremoricaContextConfiguration.class, CoreHelper.class})
public class LocalServerTimeoutTest {

    private static ChromeDriver driver;

    @Autowired
    private ServletWebServerApplicationContext webServerApplicationContext;

    @Autowired
    private CoreHelper coreHelper;


    @BeforeAll
    public static void beforeAll() throws IOException, ApiException {
        System.out.println("----------------------------------------------------------------");
        System.out.println("ⓧ Before All");
        System.out.println("----------------------------------------------------------------");
        DebugTestUtil.debugPrepare();

        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--no-sandbox");
        if (!DebugTestUtil.TEST_HEADLESS)
            chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("disable-gpu");
        driver = new ChromeDriver(chromeOptions);
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(1920, 1080));

    }

    @AfterAll
    public static void afterAll() {
        System.out.println("----------------------------------------------------------------");
        System.out.println("ⓧ After All");
        System.out.println("----------------------------------------------------------------");

        MLang.tryThis(() -> driver.quit()).onFailure(e -> LOGGER.error("Error on quit", e));
        AremoricaK8sService.stop();
    }

    @BeforeEach
    public void beforeEach(TestInfo testInfo) {
        System.out.println("----------------------------------------------------------------");
        var name = testInfo.getTestMethod().map(Method::getName).orElse("unknown");
        System.out.println("ⓧ Start Test: " + name);
        System.out.println("----------------------------------------------------------------");
    }

    @AfterEach
    public void afterEach(TestInfo testInfo) {
        System.out.println("----------------------------------------------------------------");
        var name = testInfo.getTestMethod().map(Method::getName).orElse("unknown");
        System.out.println("ⓧ End Test: " + name);
        System.out.println("----------------------------------------------------------------");
        DebugTestUtil.debugBreakpoint("After " + name);
    }

    @Test
    @Order(2)
    public void testIdleNotification() {
        App.resetUi(driver, webServerApplicationContext);
        LOGGER.info("Wait for timeout time");
        MThread.sleep(20 * 1000);
        LOGGER.info("Wait for Idle Notification Dialog");




        DebugTestUtil.doScreenshot(driver, "cluster_select");

    }

}