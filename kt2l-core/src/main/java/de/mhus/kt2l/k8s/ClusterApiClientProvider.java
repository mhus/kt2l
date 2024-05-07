package de.mhus.kt2l.k8s;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;

import java.io.IOException;

public class ClusterApiClientProvider extends ApiProvider {
    private final KubeConfig kubeConfig;

    public ClusterApiClientProvider(KubeConfig kubeConfig, long timeout) {
        super(timeout);
        this.kubeConfig = kubeConfig;
    }

    @Override
    protected ApiClient createClient() throws IOException {
        return  Config.fromConfig(kubeConfig);
    }
}