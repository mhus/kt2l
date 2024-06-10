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
import io.kubernetes.client.openapi.apis.AutoscalingV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.apis.RbacAuthorizationV1Api;
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
    private BatchV1Api batchV1Api;
    private NetworkingV1Api networkingV1Api;
    private RbacAuthorizationV1Api rbacV1Api;
    private AutoscalingV1Api autoscalingV1Api;

    protected ApiProvider(long timeout) {
        this.timeout = timeout;
    }

    public CoreV1Api getCoreV1Api() {
        getClient();
        if (coreV1Api == null)
            coreV1Api = new CoreV1Api(getClient());
        return coreV1Api;
    }
//
//    private <T> T createProxy(T api) {
//        return (T)Proxy.newProxyInstance(api.getClass().getClassLoader(), new Class[]{api.getClass()}, new InvocationHandler() {
//
//            private final Map<String, Method> methods = new HashMap<>();
//            private Object target;
//
//            {
//                target = api;
//                for(Method method: target.getClass().getDeclaredMethods()) {
//                    this.methods.put(method.getName(), method);
//                }
//            }
//
//            @Override
//            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//                try {
//                    Object result = methods.get(method.getName()).invoke(target, args);
//                    return result;
//                } catch (InvocationTargetException e) {
//                    if (e.getCause() != null && e.getCause() instanceof ApiException apiException) {
//                        LOGGER.warn("ApiException RC {}, Body {}", apiException.getCode(), apiException.getResponseBody());
//                        invalidate();
//                    }
//                    throw e;
//                }
//            }
//        });
//    }

    public RbacAuthorizationV1Api getRbacAuthorizationV1Api() {
        getClient();
        if (rbacV1Api == null)
            rbacV1Api = new RbacAuthorizationV1Api(getClient());
        return rbacV1Api;
    }

    public AppsV1Api getAppsV1Api() {
        getClient();
        if (appsV1Api == null)
            appsV1Api = new AppsV1Api(getClient());
        return appsV1Api;
    }

    public BatchV1Api getBatchV1Api() {
        getClient();
        if (batchV1Api == null)
            batchV1Api = new BatchV1Api(getClient());
        return batchV1Api;
    }

    public AutoscalingV1Api getAutoscalingV1Api() {
        getClient();
        if (autoscalingV1Api == null)
            autoscalingV1Api = new AutoscalingV1Api(getClient());
        return autoscalingV1Api;
    }

    public NetworkingV1Api getNetworkingV1Api() {
        getClient();
        if (networkingV1Api == null)
            networkingV1Api = new NetworkingV1Api(getClient());
        return networkingV1Api;
    }

    public synchronized ApiClient getClient() {
        if (client == null || System.currentTimeMillis() > refreshAt) {
            invalidate();
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
            batchV1Api = null;
            networkingV1Api = null;
            rbacV1Api = null;
            autoscalingV1Api = null;
        }
        return client;
    }

    protected abstract ApiClient createClient() throws IOException;

    public void invalidate() {
        refreshAt = 0;
    }
}
