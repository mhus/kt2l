package de.mhus.kt2l.resources.generic;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiResponse;
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenericObjectsApi {
    private final ApiClient client;
    private final V1APIResource resourceType;

    public GenericObjectsApi(ApiClient apiClient, V1APIResource resourceType) {
        client = apiClient;
        this.resourceType = resourceType;
    }

    public GenericObjectList listNamespacedCustomObject() {
//        var call = new CallBuilder()
//                .withPath("/apis/kt2l.mhus.de/v1/namespaces/default/genericobjects")
//                .withMethod("GET")
//                .build();
//        client.execute(call);
        try {
            var path = "/apis/" + resourceType.getGroup() + "/" + resourceType.getVersion() + "/namespaces/default/" + resourceType.getKind().toLowerCase() + "s";
            var call = client.buildCall(client.getBasePath(), path, "GET", null, null, null, null, null, null, null, null);
            ApiResponse<GenericObjectList> response = client.execute(call, GenericObjectList.class);
            return response.getData();
        } catch (Exception e) {
            LOGGER.error("Error while listing custom objects", e);
        }
        return new GenericObjectList();
    }
}
