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
package de.mhus.kt2l.util;

import de.mhus.commons.tools.MThread;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

import static java.time.Duration.ofSeconds;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

@Slf4j
public class App {

    public static void resetUi(ChromeDriver driver, ServletWebServerApplicationContext webServerApplicationContext) {
        LOGGER.info("Reset test on port {}", webServerApplicationContext.getWebServer().getPort());

        for (int i = 0; i < 30; i++) {
            driver.get("about:blank");
            MThread.sleep(300);
            driver.get("http://localhost:" + webServerApplicationContext.getWebServer().getPort() + "/reset");
            try {
                new WebDriverWait(driver, ofSeconds(10), ofSeconds(2))
                        .until(visibilityOfElementLocated(By.xpath("//vaadin-button[contains(.,\"KT2L\")]")));
                break;
            } catch (Exception e) {
                LOGGER.error("Reset reset failed", e);
                throw new RuntimeException("Reset reset failed");
            }
        }

        for (int i = 0; i < 30; i++) {
            driver.get("about:blank");
            MThread.sleep(300);
            driver.get("http://localhost:" + webServerApplicationContext.getWebServer().getPort());
            try {
                new WebDriverWait(driver, ofSeconds(10), ofSeconds(2))
                        .until(visibilityOfElementLocated(By.xpath("//span[contains(.,\"[KT2L]\")]")));
                return;
            } catch (Exception e) {
                LOGGER.error("Reset home failed", e);
            }
        }
        throw new RuntimeException("Reset home failed");
    }

    // Open the cluster resources view from main view
    public static void clusterOpenResources(ChromeDriver driver) {
        // click on Resources on Main
        driver.findElement(By.xpath("//vaadin-menu-bar-item[contains(.,\"Resources\")]")).click();
        // wait for the view menu
        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(presenceOfElementLocated(By.xpath("//vaadin-menu-bar-item[contains(.,\"View\")]")));
    }

    public static void resourcesSelectNamespace(ChromeDriver driver, String namespace) {
        // select namespace
        {
            var input = driver.findElement(By.xpath("//vaadin-combo-box[@placeholder=\"Namespace\"]/input"));
            input.clear();
            input.sendKeys("indomitable-village");
            MThread.sleep(1000);  // timing shit
            input.sendKeys(Keys.DOWN, Keys.RETURN);
        }
    }


    public static void resourcesSelectResource(ChromeDriver driver, String resource) {
        {
            var input = driver.findElement(By.xpath("//vaadin-combo-box[@placeholder=\"Resource\"]/input"));
            input.clear();
            input.sendKeys(resource);
            MThread.sleep(1000);  // timing shit
            input.sendKeys(Keys.DOWN, Keys.RETURN);
        }
    }
}
