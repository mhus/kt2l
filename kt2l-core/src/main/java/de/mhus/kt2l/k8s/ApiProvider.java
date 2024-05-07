package de.mhus.kt2l.k8s;

import de.mhus.commons.errors.InternalRuntimeException;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public abstract class ApiProvider {

    public static final long DEFAULT_TIMEOUT = 1000 * 60 * 5;
    private long refreshAt = 0;
    private ApiClient client = null;
    private final long timeout;
    private CoreV1Api coreV1Api;

    protected ApiProvider(long timeout) {
        this.timeout = timeout;
    }

    public CoreV1Api getCoreV1Api() {
        if (coreV1Api == null)
            coreV1Api = new CoreV1Api(getClient());
        return coreV1Api;
    }

    public ApiClient getClient() {
        if (client == null || System.currentTimeMillis() > refreshAt) {
            try {
                LOGGER.debug("Create new cluster client");
                client = createClient();
                refreshAt = System.currentTimeMillis() + timeout;
            } catch (IOException e) {
                LOGGER.error("Can't create client", e);
                throw new InternalRuntimeException(e);
            }
            coreV1Api = null;
        }
        return client;
    }

    protected abstract ApiClient createClient() throws IOException;
}
