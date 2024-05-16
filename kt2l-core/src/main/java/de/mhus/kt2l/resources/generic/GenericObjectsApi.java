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
package de.mhus.kt2l.resources.generic;

import de.mhus.commons.errors.InternalRuntimeException;
import de.mhus.kt2l.k8s.CallBackAdapter;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiResponse;
import io.kubernetes.client.openapi.Pair;
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class GenericObjectsApi {
    private final V1APIResource resourceType;

    @Setter @Getter
    private String localCustomBaseUrl;
    @Setter @Getter
    private int localHostIndex;
    @Setter @Getter
    private ApiClient localVarApiClient;

    public GenericObjectsApi(ApiClient apiClient, V1APIResource resourceType) {
        localVarApiClient = apiClient;
        this.resourceType = resourceType;
    }

    public GenericObjectList listNamespacedCustomObject(String namespace) {

        try {
            String basePath = null;
            // Operation Servers
            String[] localBasePaths = new String[] {  };

            // Determine Base Path to Use
            if (localCustomBaseUrl != null){
                basePath = localCustomBaseUrl;
            } else if ( localBasePaths.length > 0 ) {
                basePath = localBasePaths[localHostIndex];
            } else {
                basePath = "";
            }

            Object localVarPostBody = null;

            // create path and map variables
            String localVarPath = createPath(resourceType, namespace);

            List<Pair> localVarQueryParams = new ArrayList<Pair>();
            List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
            Map<String, String> localVarHeaderParams = new HashMap<String, String>();
            Map<String, String> localVarCookieParams = new HashMap<String, String>();
            Map<String, Object> localVarFormParams = new HashMap<String, Object>();

            final String[] localVarAccepts = {
                    "application/json",
                    "application/yaml",
                    "application/vnd.kubernetes.protobuf"
            };
            final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
            if (localVarAccept != null) {
                localVarHeaderParams.put("Accept", localVarAccept);
            }

            final String[] localVarContentTypes = {
            };
            final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
            if (localVarContentType != null) {
                localVarHeaderParams.put("Content-Type", localVarContentType);
            }

            String[] localVarAuthNames = new String[] { "BearerToken" };

            LOGGER.debug("List custom objects: {}", localVarPath);
            var callback = new CallBackAdapter(LOGGER);
            var call = localVarApiClient.buildCall(basePath + localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, callback);

            ApiResponse<GenericObjectList> response = localVarApiClient.execute(call, GenericObjectList.class);
            return response.getData();
        } catch (Exception e) {
            LOGGER.error("Error while listing custom objects", e);
        }
        return new GenericObjectList();
    }

    private String createPath(V1APIResource r, String namespace) {
        // v1/pods
        // apps/v1/daemonsets
        // storage.k8s.io/v1/csidrivers
//        /api/v1/namespaces
//        /api/v1/pods
//        /api/v1/namespaces/my-namespace/pods
        // <api(s)> / <group> / <version> / <resource> / <namespace> / <name>
//        /apis/apps/v1/deployments
//        /apis/apps/v1/namespaces/my-namespace/deployments
//        /apis/apps/v1/namespaces/my-namespace/deployments/my-deployment

        var path = new StringBuffer();
        if (r.getGroup() != null) {
            path.append("/apis/").append(r.getGroup());
            if (r.getVersion() != null) {
                path.append("/").append(r.getVersion());
            } else {
                path.append("/v1");
            }
            if (r.getNamespaced() && namespace != null) {
                path.append("/namespaces/").append(namespace).append("/").append(r.getName());
            } else {
                path.append("/").append(r.getName());
            }
        } else {
            path.append("/api");
            if (r.getVersion() != null) {
                path.append("/").append(r.getVersion());
            } else {
                path.append("/v1");
            }
            if (r.getNamespaced() && namespace != null) {
                path.append("/namespaces/").append(namespace).append("/").append(r.getName());
            } else {
                path.append("/").append(r.getName());
            }
        }
        return path.toString();
    }

    public GenericObject replace(String name, String namespace, String yaml) {
        try {
            //XXX yaml2json !!!!
            // MYaml.toJson(yaml);
            String basePath = null;
            // Operation Servers
            String[] localBasePaths = new String[] {  };

            // Determine Base Path to Use
            if (localCustomBaseUrl != null){
                basePath = localCustomBaseUrl;
            } else if ( localBasePaths.length > 0 ) {
                basePath = localBasePaths[localHostIndex];
            } else {
                basePath = null;
            }

            Object localVarPostBody = null;

            // create path and map variables
            String localVarPath = createPath(resourceType, namespace) + "/" + name;

            List<Pair> localVarQueryParams = new ArrayList<Pair>();
            List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
            Map<String, String> localVarHeaderParams = new HashMap<String, String>();
            Map<String, String> localVarCookieParams = new HashMap<String, String>();
            Map<String, Object> localVarFormParams = new HashMap<String, Object>();

            final String[] localVarAccepts = {
                    "application/json",
                    "application/yaml",
                    "application/vnd.kubernetes.protobuf"
            };
            final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
            if (localVarAccept != null) {
                localVarHeaderParams.put("Accept", localVarAccept);
            }

            final String[] localVarContentTypes = {
            };
            final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
            if (localVarContentType != null) {
                localVarHeaderParams.put("Content-Type", localVarContentType);
            }

            String[] localVarAuthNames = new String[] { "BearerToken" };

            LOGGER.debug("List custom objects: {}", localVarPath);
            var callback = new CallBackAdapter(LOGGER);
            var call = localVarApiClient.buildCall(basePath + localVarPath, "PUT", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, callback);

            ApiResponse<GenericObject> response = localVarApiClient.execute(call, GenericObject.class);
            return response.getData();
        } catch (Exception e) {
            LOGGER.error("Error while listing custom objects", e);
            throw new InternalRuntimeException(e);
        }
    }

    public GenericObject delete(String name, String namespace) {
        try {
            String basePath = null;
            // Operation Servers
            String[] localBasePaths = new String[] {  };

            // Determine Base Path to Use
            if (localCustomBaseUrl != null){
                basePath = localCustomBaseUrl;
            } else if ( localBasePaths.length > 0 ) {
                basePath = localBasePaths[localHostIndex];
            } else {
                basePath = null;
            }

            Object localVarPostBody = null;

            // create path and map variables
            String localVarPath = createPath(resourceType, namespace) + "/" + name;

            List<Pair> localVarQueryParams = new ArrayList<Pair>();
            List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
            Map<String, String> localVarHeaderParams = new HashMap<String, String>();
            Map<String, String> localVarCookieParams = new HashMap<String, String>();
            Map<String, Object> localVarFormParams = new HashMap<String, Object>();

            final String[] localVarAccepts = {
                    "application/json",
                    "application/yaml",
                    "application/vnd.kubernetes.protobuf"
            };
            final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
            if (localVarAccept != null) {
                localVarHeaderParams.put("Accept", localVarAccept);
            }

            final String[] localVarContentTypes = {
            };
            final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
            if (localVarContentType != null) {
                localVarHeaderParams.put("Content-Type", localVarContentType);
            }

            String[] localVarAuthNames = new String[] { "BearerToken" };

            LOGGER.debug("List custom objects: {}", localVarPath);
            var callback = new CallBackAdapter(LOGGER);
            var call = localVarApiClient.buildCall(basePath + localVarPath, "DELETE", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, callback);

            ApiResponse<GenericObject> response = localVarApiClient.execute(call, GenericObject.class);
            return response.getData();
        } catch (Exception e) {
            LOGGER.error("Error while listing custom objects", e);
            throw new InternalRuntimeException(e);
        }
    }

    public Object create(String yaml) {
        //XXX
        return null;
    }
}
