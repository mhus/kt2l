package de.mhus.kt2l.k8s;

import io.kubernetes.client.util.KubeConfig;

public interface KubeConfigProvider {
        KubeConfig getKubeConfig();
}
