/*
 * kt2l-core - kt2l core implementation
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
package de.mhus.kt2l.k8s;

import de.mhus.commons.errors.InternalRuntimeException;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
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
    private AppsV1Api appsV1Api;

    protected ApiProvider(long timeout) {
        this.timeout = timeout;
    }

    public CoreV1Api getCoreV1Api() {
        if (coreV1Api == null)
            coreV1Api = new CoreV1Api(getClient());
        return coreV1Api;
    }

    public AppsV1Api getAppsV1Api() {
        if (appsV1Api == null)
            appsV1Api = new AppsV1Api(getClient());
        return appsV1Api;
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
            appsV1Api = null;
        }
        return client;
    }

    protected abstract ApiClient createClient() throws IOException;
}
