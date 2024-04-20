package de.mhus.kt2l;

import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.SpringSecurityAutoConfiguration;
import com.vaadin.flow.spring.VaadinScopesConfig;
import de.mhus.commons.console.Console;
import de.mhus.commons.tools.MCast;
import de.mhus.commons.tools.MLang;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.k8s.K8sService;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

import javax.swing.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.Set;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openqa.selenium.support.ui.ExpectedConditions.titleIs;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContextConfiguration.class)
public class WhatTest {

    private static K3sContainer k3s;
    private static ChromeDriver driver;

    @Autowired
    private ServletWebServerApplicationContext webServerAppCtxt;

    @BeforeAll
    public static void beforeAll() throws IOException {
        System.out.println("Before All");
        if (Kt2lTestApplication.DEBUG) {
            var frame = new JFrame();
            frame.setSize(200,50);
            frame.getContentPane().add(new JLabel("Test in debug mode"));
            frame.setVisible(true);
        }
        k3s = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.21.3-k3s1"))
                .withLogConsumer(new Slf4jLogConsumer(LOGGER));
        k3s.start();
        String kubeConfigYaml = k3s.getKubeConfigYaml();
        ApiClient client = Config.fromConfig(new StringReader(kubeConfigYaml));
        CoreV1Api api = new CoreV1Api(client);

//        WebDriverManager.chromedriver().clearDriverCache().setup();
//        WebDriverManager.chromedriver().clearResolutionCache().setup();

        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();

    }

    @AfterAll
    public static void afterAll() {
        System.out.println("After All");

        MLang.tryThis(() -> driver.quit()).onError(e -> LOGGER.error("Error on quit", e));
        MLang.tryThis(() -> k3s.stop()).onError(e -> LOGGER.error("Error on quit", e));
    }

    @Test
    @Order(1)
    public void testK8sInit() {

    }

        @Test
    @Order(2)
    public void testLogin() {

        driver.get("http://localhost:"+webServerAppCtxt.getWebServer().getPort());
        LOGGER.info("Start Test on port {}", webServerAppCtxt.getWebServer().getPort());
        MThread.sleep(1000 * 2);
        driver.get("http://localhost:"+webServerAppCtxt.getWebServer().getPort());

        new WebDriverWait(driver, ofSeconds(60), ofSeconds(1))
                .until(titleIs("KT2L"));

        if (Kt2lTestApplication.DEBUG) {
            JOptionPane.showConfirmDialog(null,
                    "Click ok if you are ok", "Be ok!", JOptionPane.DEFAULT_OPTION);
        }

    }

    public static class MyK8sService extends K8sService {

        public Set<String> getAvailableContexts() {
            return Set.of("test");
        }

        public ApiClient getKubeClient(String contextName) throws IOException {
            if ("test".equals(contextName)) {
                return Config.fromConfig(new StringReader(k3s.getKubeConfigYaml()));
            }
            throw new IOException("Context not found");
        }

        }

}
