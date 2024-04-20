package de.mhus.kt2l.util;

import de.mhus.kt2l.k8s.K8sService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class AremoricaContextConfiguration {

    @Bean
    public K8sService k8sService() {
        return new AremoricaK8sService();
    }

}
