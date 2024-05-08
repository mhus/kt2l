/*
 * kt2l - KT2L (ktool) is a web based tool to manage your kubernetes clusters.
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
import de.mhus.commons.util.Value;
import de.mhus.kt2l.resources.ResourcesGridPanel;
import de.mhus.kt2l.resources.pod.PodGrid;
import de.mhus.kt2l.util.AremoricaContextConfiguration;
import de.mhus.kt2l.util.AremoricaK8sService;
import de.mhus.kt2l.util.CoreHelper;
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
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.util.Set;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.titleIs;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"configuration.localDirectory=local_config"}
)
@Import({AremoricaContextConfiguration.class, CoreHelper.class})
public class LocalServerTest {

    private static ChromeDriver driver;

    @Autowired
    private ServletWebServerApplicationContext webServerApplicationContext;

    @Autowired
    private CoreHelper coreHelper;


    @BeforeAll
    public static void beforeAll() throws IOException, ApiException {
        System.out.println("Before All");
        DebugTestUtil.debugPrepare();
        AremoricaK8sService.start();

//        WebDriverManager.chromedriver().clearDriverCache().setup();
//        WebDriverManager.chromedriver().clearResolutionCache().setup();

        WebDriverManager.chromedriver().setup();
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--no-sandbox");
        if (!DebugTestUtil.TEST_HEADLESS)
            chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("disable-gpu");
        driver = new ChromeDriver(chromeOptions);
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(1920, 1080));

        AremoricaK8sService.createAremorica();

    }

    @AfterAll
    public static void afterAll() {
        System.out.println("After All");

        MLang.tryThis(() -> driver.quit()).onFailure(e -> LOGGER.error("Error on quit", e));
        AremoricaK8sService.stop();
    }

    public void resetUi() {
        LOGGER.info("Reset test on port {}", webServerApplicationContext.getWebServer().getPort());

        for (int i = 0; i < 4; i++) {
            driver.get("http://localhost:" + webServerApplicationContext.getWebServer().getPort() + "/reset");
            try {
                new WebDriverWait(driver, ofSeconds(4), ofSeconds(1))
                        .until(visibilityOfElementLocated(By.xpath("//vaadin-button[contains(.,\"KT2L\")]")));
                break;
            } catch (Exception e) {
                LOGGER.error("Reset reset failed", e);
                throw new RuntimeException("Reset reset failed");
            }
        }

        for (int i = 0; i < 4; i++) {
            driver.get("http://localhost:" + webServerApplicationContext.getWebServer().getPort());
            try {
                new WebDriverWait(driver, ofSeconds(4), ofSeconds(1))
                        .until(visibilityOfElementLocated(By.xpath("//span[contains(.,\"[KT2L]\")]")));
                return;
            } catch (Exception e) {
                LOGGER.error("Reset home failed", e);
            }
        }
        throw new RuntimeException("Reset home failed");
    }

    @Test
    @Order(2)
    public void testClusterSelect() {
        resetUi();

        // Cluster Name
        var clusterSelector = driver.findElement(By.id("input-vaadin-combo-box-4"));
        assertThat(clusterSelector).isNotNull();
        assertThat(clusterSelector.getAttribute("value")).isEqualTo("Aremorica");
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(titleIs("KT2L"));

        assertThat(driver.findElement(By.xpath("//vaadin-menu-bar-item[contains(.,\"Resources\")]"))).isNotNull();

        DebugTestUtil.doScreenshot(driver, "cluster_select");
        DebugTestUtil.debugBreakpoint("Cluster Select");

    }

    @Test
    @Order(3)
    public void testNamespacePush() throws InterruptedException, ApiException {
        resetUi();

        // click on Resources on Main
        driver.findElement(By.xpath("//vaadin-menu-bar-item[contains(.,\"Resources\")]")).click();
        // wait for the view menu
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(presenceOfElementLocated(By.xpath("//vaadin-menu-bar-item[contains(.,\"View\")]")));
        // select namespace
        {
            var input = driver.findElement(By.xpath("//vaadin-combo-box[@placeholder=\"Resource\"]/input"));
            input.clear();
            input.sendKeys("namespace");
            input.sendKeys(Keys.DOWN, Keys.RETURN);
        }
        // wait for the namespace grid
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(presenceOfElementLocated(By.xpath("//vaadin-grid-cell-content[contains(.,\"indomitable-village\")]")));
        // validate little-bonum is not there
        var core = coreHelper.getLastCore();
        ResourcesGridPanel grid = (ResourcesGridPanel) core.getTabBar().getTab("test/aremorica").get().getPanel();
        assertThat(grid.getNamespaces().contains("little-bonum")).isFalse();
        assertThatThrownBy(() -> driver.findElement(By.xpath("//vaadin-grid-cell-content[contains(.,\"little-bonum\")]")))
                .isInstanceOf(NoSuchElementException.class);
        // create namespace little-bonum
        AremoricaK8sService.createNamespace("little-bonum");
        // wait for the namespace to appear
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until( (d) -> grid.getNamespaces().contains("little-bonum"));
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(presenceOfElementLocated(By.xpath("//vaadin-grid-cell-content[contains(.,\"little-bonum\")]")));

        DebugTestUtil.doScreenshot(driver, "cluster_resources_namespaces");

        // delete namespace little-bonum
        AremoricaK8sService.deleteNamespace("little-bonum");
        // wait for the namespace to disappear
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until( (d) -> !grid.getNamespaces().contains("little-bonum"));
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(invisibilityOfElementLocated(By.xpath("//vaadin-grid-cell-content[contains(.,\"little-bonum\")]")));

        DebugTestUtil.debugBreakpoint("Namespace Push");

    }


    @Test
    @Order(4)
    public void testPodPush() throws InterruptedException, ApiException {
        resetUi();

        // click on Resources on Main
        driver.findElement(By.xpath("//vaadin-menu-bar-item[contains(.,\"Resources\")]")).click();
        // wait for the view menu
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(presenceOfElementLocated(By.xpath("//vaadin-menu-bar-item[contains(.,\"View\")]")));
        // select pods
        {
            var input = driver.findElement(By.xpath("//vaadin-combo-box[@placeholder=\"Resource\"]/input"));
            input.clear();
            input.sendKeys("pods");
            input.sendKeys(Keys.DOWN, Keys.RETURN);
        }
        // select namespace
        {
            var input = driver.findElement(By.xpath("//vaadin-combo-box[@placeholder=\"Namespace\"]/input"));
            input.clear();
            input.sendKeys("indomitable-village");
            input.sendKeys(Keys.DOWN, Keys.RETURN);
        }

        // wait for the namespace grid
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(presenceOfElementLocated(By.xpath("//vaadin-grid-cell-content[contains(.,\"asterix\")]")));
        // validate idefix is not there
        var core = coreHelper.getLastCore();
        ResourcesGridPanel grid = (ResourcesGridPanel) core.getTabBar().getTab("test/aremorica").get().getPanel();
        PodGrid podGrid = (PodGrid)grid.getGrid();
        assertThat(podGrid.getFilteredList().stream().map(pod -> pod.getName())).doesNotContain("idefix");
        assertThatThrownBy(() -> driver.findElement(By.xpath("//vaadin-grid-cell-content[contains(.,\"idefix\")]")))
                .isInstanceOf(NoSuchElementException.class);
        // create pod idefix
        AremoricaK8sService.createPod("idefix", "indomitable-village", "mhus/example-dice:20240503", "INFINITE", "true");
        // wait for the pod to appear
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until( (d) -> podGrid.getFilteredList().stream().map(pod -> pod.getName()).toList().contains("idefix"));
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(presenceOfElementLocated(By.xpath("//vaadin-grid-cell-content[contains(.,\"idefix\")]")));
        AremoricaK8sService.waitForPodReady("idefix", "indomitable-village");

        DebugTestUtil.doScreenshot(driver, "cluster_resources_pods");

        // delete pod idefix
        AremoricaK8sService.deletePod("idefix", "indomitable-village");
        // wait for the namespace to disappear
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until( (d) -> !podGrid.getFilteredList().stream().map(pod -> pod.getName()).toList().contains("idefix"));
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(invisibilityOfElementLocated(By.xpath("//vaadin-grid-cell-content[contains(.,\"idefix\")]")));

        DebugTestUtil.debugBreakpoint("Pod Push");

    }


    @Test
    @Order(5)
    public void testPodDetails() throws InterruptedException {
        resetUi();

        // click on Resources on Main
        driver.findElement(By.xpath("//vaadin-menu-bar-item[contains(.,\"Resources\")]")).click();
        // wait for the view menu
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(presenceOfElementLocated(By.xpath("//vaadin-menu-bar-item[contains(.,\"View\")]")));

        // click on pod asterix
        driver.findElement(By.xpath("//vaadin-grid-cell-content[contains(.,\"asterix\")]")).click();

//        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));

        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1)).until((d) ->
        {
            try {
                var element = d.findElement(By.xpath("//vaadin-grid-cell-content[contains(.,\"asterix\")]/preceding-sibling::*[2]/vaadin-checkbox"));
                return element.getAttribute("checked") != null;
            } catch (Exception e) {
                return false;
            }
        });

//        new WebDriverWait(driver, ofSeconds(600), ofSeconds(1))
//                .until(ExpectedConditions.attributeToBe(By.xpath("//vaadin-grid-cell-content[contains(.,\"asterix\")]/preceding-sibling::*[2]/vaadin-checkbox"), "checked", ""));

//        vaadinUiHelper.
        // should be selected
//XXX        assertThat(driver.findElement(By.xpath("//vaadin-grid-cell-content[contains(.,\"asterix\")]/../..")).getAttribute("selected")).isEqualTo("true");


        var core = coreHelper.getLastCore();
        ResourcesGridPanel grid = (ResourcesGridPanel) core.getTabBar().getTab("test/aremorica").get().getPanel();

        Value<Set<PodGrid.Pod>> selected = new Value<>();
        core.getUI().get().access(() -> {
            selected.value = ((PodGrid)grid.getGrid()).getResourcesGrid().getSelectedItems();
            LOGGER.debug("Selected: {}", selected.value);
        });
        MLang.await(() -> selected.value, 5000);

        assertThat(selected.value).isNotNull();
        assertThat(selected.value.size()).isEqualTo(1);

//        DebugTestUtil.doScreenshot(driver, "cluster_resources_pod");
        DebugTestUtil.debugBreakpoint("Pod Details");

    }

}