package de.mhus.kt2l.k8s;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.Config;

import java.io.IOException;

public class DefaultClientApiClientProvider extends ApiProvider {


    protected DefaultClientApiClientProvider(long timeout) {
        super(timeout);
    }

    @Override
    protected ApiClient createClient() throws IOException {
        return Config.defaultClient();
    }
}
