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
import de.mhus.kt2l.util.WebDriverUtil;
import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
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

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
        "kt2l.configuration.localDirectory=config_local",
        "kt2l.configuration.usersDirectory=users_nodirectoryset",
        "kt2l.deskTabPreserveMode=false"
        }
)
@Import({AremoricaContextConfiguration.class, CoreHelper.class})
public class LocalServerTest {

    private static WebDriver driver;

    @Autowired
    private ServletWebServerApplicationContext webServerApplicationContext;

    @Autowired
    private CoreHelper coreHelper;


    @BeforeAll
    public static void beforeAll() throws IOException, ApiException {
        System.out.println("----------------------------------------------------------------");
        System.out.println("Ⓘ Before All");
        System.out.println("----------------------------------------------------------------");
        DebugTestUtil.debugPrepare();
        AremoricaK8sService.start();

        driver = WebDriverUtil.open();

        AremoricaK8sService.createAremorica();

    }

    @AfterAll
    public static void afterAll() {
        System.out.println("----------------------------------------------------------------");
        System.out.println("Ⓘ After All");
        System.out.println("----------------------------------------------------------------");

        WebDriverUtil.close(driver);
        AremoricaK8sService.stop();
    }

    @BeforeEach
    public void beforeEach(TestInfo testInfo) {
        System.out.println("----------------------------------------------------------------");
        var name = testInfo.getTestMethod().map(Method::getName).orElse("unknown");
        System.out.println("Ⓘ Start Test: " + name);
        System.out.println("----------------------------------------------------------------");
    }

    @AfterEach
    public void afterEach(TestInfo testInfo) {
        System.out.println("----------------------------------------------------------------");
        var name = testInfo.getTestMethod().map(Method::getName).orElse("unknown");
        System.out.println("Ⓘ End Test: " + name);
        System.out.println("----------------------------------------------------------------");
        DebugTestUtil.debugBreakpoint("After " + name);
    }

    @Test
    @Order(2)
    public void testClusterSelect() {
        App.resetUi(driver, webServerApplicationContext);

        // Cluster Name
        var clusterSelector = driver.findElement(By.cssSelector("#clusterselect input"));
        assertThat(clusterSelector).isNotNull();
        assertThat(clusterSelector.getAttribute("value")).isEqualTo("Aremorica");
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(titleIs("KT2L"));

        assertThat(driver.findElement(By.xpath("//vaadin-menu-bar-item[contains(.,\"Resources\")]"))).isNotNull();

        DebugTestUtil.doScreenshot(driver, "cluster_select");

    }

    @Test
    @Order(3)
    public void testNamespacePush() throws InterruptedException, ApiException {
        App.resetUi(driver, webServerApplicationContext);

        // click on Resources on Main
        App.clusterOpenResources(driver);

        // select namespace
        App.resourcesSelectResource(driver, "namespace ");
        // wait for the namespace grid
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(presenceOfElementLocated(By.xpath("//vaadin-grid-cell-content[contains(.,\"indomitable-village\")]")));

       // validate little-bonum is not there
        var core = coreHelper.getLastCore();
        ResourcesGridPanel grid = (ResourcesGridPanel) core.getTabBar().getTab("aremorica:resources").get().getPanel();
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

    }


    @Test
    @Order(4)
    public void testPodPush() throws InterruptedException, ApiException {
        App.resetUi(driver, webServerApplicationContext);

        // click on Resources on Main
        App.clusterOpenResources(driver);

        // select pods
        App.resourcesSelectResource(driver, "pod ");
        // select namespace
        App.resourcesSelectNamespace(driver, "indomitable-village");
        // wait for the namespace grid
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(presenceOfElementLocated(By.xpath("//vaadin-grid-cell-content[contains(.,\"asterix\")]")));

        // validate idefix is not there
        var core = coreHelper.getLastCore();
        ResourcesGridPanel grid = (ResourcesGridPanel) core.getTabBar().getTab("aremorica:resources").get().getPanel();
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

    }


    @Test
    @Order(5)
    public void testAceEditor() throws InterruptedException {
        App.resetUi(driver, webServerApplicationContext);

        // click on Resources on Main
        App.clusterOpenResources(driver);

        // click on pod asterix
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(presenceOfElementLocated(By.xpath("//vaadin-grid-cell-content[contains(.,\"asterix\")]")));
        driver.findElement(By.xpath("//vaadin-grid-cell-content[contains(.,\"asterix\")]")).click();

        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1)).until((d) ->
        {
            try {
                var element = d.findElement(By.xpath("//vaadin-grid-cell-content[contains(.,\"asterix\")]/preceding-sibling::*[2]/vaadin-checkbox"));
                return element.getAttribute("checked") != null;
            } catch (Exception e) {
                return false;
            }
        });

        var core = coreHelper.getLastCore();
        ResourcesGridPanel grid = (ResourcesGridPanel) core.getTabBar().getTab("aremorica:resources").get().getPanel();

        Value<Set<PodGrid.Resource>> selected = new Value<>();
        core.getUI().get().access(() -> {
            selected.value = ((PodGrid)grid.getGrid()).getResourcesGrid().getSelectedItems();
            LOGGER.debug("Selected: {}", selected.value);
        });
        MLang.await(() -> selected.value, 5000);

        assertThat(selected.value).isNotNull();
        assertThat(selected.value.size()).isEqualTo(1);

        // send 'y' for Yaml Editor
        {
            var element = driver.findElement(By.xpath("//body"));
            element.sendKeys("y");
        }
        // wait for editor
        //  driver.findElement(By.cssSelector("lit-ace")).getShadowRoot().findElement(
        //        By.cssSelector("span.ace_tag")).getText()
        MThread.sleep(1000);
        var element = new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(
                        presenceOfElementLocated(By.cssSelector("lit-ace"))
                );

        var textElement = element.getShadowRoot().findElement(By.cssSelector("span.ace_tag"));
        assertThat(textElement.getText()).isEqualTo("metadata");

        DebugTestUtil.doScreenshot(driver, "pod_yaml_editor");
    }

    @Test
    @Order(6)
    public void testXTermAddon() throws InterruptedException {
        App.resetUi(driver, webServerApplicationContext);

        // click on Resources on Main
        App.clusterOpenResources(driver);

        // click on pod asterix
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(presenceOfElementLocated(By.xpath("//vaadin-grid-cell-content[contains(.,\"asterix\")]")));
        driver.findElement(By.xpath("//vaadin-grid-cell-content[contains(.,\"asterix\")]")).click();

        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1)).until((d) ->
        {
            try {
                var element = d.findElement(By.xpath("//vaadin-grid-cell-content[contains(.,\"asterix\")]/preceding-sibling::*[2]/vaadin-checkbox"));
                return element.getAttribute("checked") != null;
            } catch (Exception e) {
                return false;
            }
        });

        // send 's' for Shell
        {
            var element = driver.findElement(By.xpath("//body"));
            element.sendKeys("s");
        }
        // wait for shell
        {
            var element = new WebDriverWait(driver, ofSeconds(10), ofSeconds(1))
                    .until(visibilityOfElementLocated(By.xpath("//vaadin-vertical-layout[@id=\"aremoricaindomitable-villageasterixshell\"]/fc-xterm/div/div/div[2]/div[2]/div[1]/span[1]")));
            MLang.awaitTrue(() -> element.getText().trim().length() > 0, 5000);
            assertThat(element.getText()).isEqualTo("Start console");
        }
        DebugTestUtil.doScreenshot(driver, "pod_xterm");
    }

}