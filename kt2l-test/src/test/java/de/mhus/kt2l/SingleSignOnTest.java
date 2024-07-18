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

import de.mhus.kt2l.util.App;
import de.mhus.kt2l.util.AremoricaContextConfiguration;
import de.mhus.kt2l.util.CoreHelper;
import de.mhus.kt2l.util.TestResultDebugWatcher;
import de.mhus.kt2l.util.WebDriverUtil;
import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Import;

import java.io.IOException;

import static java.time.Duration.ofSeconds;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

@Disabled
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
        "kt2l.configuration.localDirectory=config_sso",
        "kt2l.configuration.usersDirectory=users_nodirectoryset",
        "kt2l.deskTabPreserveMode=false",
        "spring.security.oauth2.client.registration.google.client-id=x",
        "spring.security.oauth2.client.registration.google.client-secret=x"
        }
)
@Import({AremoricaContextConfiguration.class, CoreHelper.class})
@ExtendWith(TestResultDebugWatcher.class)
public class SingleSignOnTest {

    private static WebDriver driver;

    @Autowired
    private ServletWebServerApplicationContext webServerApplicationContext;

    @Autowired
    private CoreHelper coreHelper;

    @BeforeAll
    public static void beforeAll() throws IOException, ApiException {
        driver = WebDriverUtil.open();
    }

    @AfterAll
    public static void afterAll() {
        WebDriverUtil.close(driver);
    }

    @Test
    public void testSingleSignOn() {
        App.resetUi(driver, webServerApplicationContext, true);

        // wait for login page
        new WebDriverWait(driver, ofSeconds(10), ofSeconds(1))
                .until(presenceOfElementLocated(By.xpath("//div[contains(.,\"Login with Google\")]")));

        new WebDriverWait(driver, ofSeconds(10), ofSeconds(1))
                .until(presenceOfElementLocated(By.xpath("//div[contains(.,\"Login with Github\")]")));

        var googleLink = driver.findElement(By.xpath("//div[text()='Login with Google']"));
        // click on google login
        googleLink.click();
        // wait for the google page - oauth id not correct, but that's fine
        new WebDriverWait(driver, ofSeconds(10), ofSeconds(1))
                .until(presenceOfElementLocated(By.xpath("//div[contains(.,\"The OAuth client was not found.\")]")));

        DebugTestUtil.doScreenshot(driver, "extend_session");

    }

}