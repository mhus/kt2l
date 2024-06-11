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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.V1ObjectMeta;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class APIGroupDiscovery implements KubernetesObject {
    public static final String SERIALIZED_NAME_API_VERSION = "apiVersion";
    @SerializedName(SERIALIZED_NAME_API_VERSION)
    private String apiVersion;

    public static final String SERIALIZED_NAME_KIND = "kind";
    @SerializedName(SERIALIZED_NAME_KIND)
    private String kind;

    public static final String SERIALIZED_NAME_METADATA = "metadata";
    @SerializedName(SERIALIZED_NAME_METADATA)
    private V1ObjectMeta metadata;

    public String getVersions() {
        return versions;
    }

    @SerializedName("versions")
    @JsonAdapter(DataDeserializer.class)
    private String versions;

    public APIGroupDiscovery() {
    }

    public APIGroupDiscovery apiVersion(String apiVersion) {

        this.apiVersion = apiVersion;
        return this;
    }

    /**
     * APIVersion defines the versioned schema of this representation of an object. Servers should convert recognized schemas to the latest internal value, and may reject unrecognized values. More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#resources
     * @return apiVersion
     **/
    @jakarta.annotation.Nullable
    public String getApiVersion() {
        return apiVersion;
    }


    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }


    public APIGroupDiscovery kind(String kind) {

        this.kind = kind;
        return this;
    }

    /**
     * Kind is a string value representing the REST resource this object represents. Servers may infer this from the endpoint the client submits requests to. Cannot be updated. In CamelCase. More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#types-kinds
     * @return kind
     **/
    @jakarta.annotation.Nullable
    public String getKind() {
        return kind;
    }


    public void setKind(String kind) {
        this.kind = kind;
    }


    public APIGroupDiscovery metadata(V1ObjectMeta metadata) {

        this.metadata = metadata;
        return this;
    }

    /**
     * Get metadata
     * @return metadata
     **/
    @jakarta.annotation.Nullable
    public V1ObjectMeta getMetadata() {
        return metadata;
    }


    public void setMetadata(V1ObjectMeta metadata) {
        this.metadata = metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        APIGroupDiscovery GenericObject = (APIGroupDiscovery) o;
        return Objects.equals(this.apiVersion, GenericObject.apiVersion) &&
                Objects.equals(this.kind, GenericObject.kind) &&
                Objects.equals(this.metadata, GenericObject.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiVersion, kind, metadata);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class APIGroupDiscovery {\n");
        sb.append("    apiVersion: ").append(toIndentedString(apiVersion)).append("\n");
        sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
        sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
        sb.append("    versions: ").append(toIndentedString(versions)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }


    public static HashSet<String> openapiFields;
    public static HashSet<String> openapiRequiredFields;

    static {
        // a set of all properties/fields (JSON key names)
        openapiFields = new HashSet<String>();
        openapiFields.add("apiVersion");
        openapiFields.add("kind");
        openapiFields.add("metadata");
        openapiFields.add("spec");
        openapiFields.add("status");

        // a set of required properties/fields (JSON key names)
        openapiRequiredFields = new HashSet<String>();
    }

    /**
     * Validates the JSON Object and throws an exception if issues found
     *
     * @param jsonObj JSON Object
     * @throws IOException if the JSON Object is invalid with respect to GenericObject
     */
    public static void validateJsonObject(JsonObject jsonObj) throws IOException {
        if (jsonObj == null) {
            if (!APIGroupDiscovery.openapiRequiredFields.isEmpty()) { // has required fields but JSON object is null
                throw new IllegalArgumentException(String.format("The required field(s) %s in GenericObject is not found in the empty JSON string", APIGroupDiscovery.openapiRequiredFields.toString()));
            }
        }

        Set<Map.Entry<String, JsonElement>> entries = jsonObj.entrySet();
        // check to see if the JSON string contains additional fields
        for (Map.Entry<String, JsonElement> entry : entries) {
            if (!APIGroupDiscovery.openapiFields.contains(entry.getKey())) {
                throw new IllegalArgumentException(String.format("The field `%s` in the JSON string is not defined in the `GenericObject` properties. JSON: %s", entry.getKey(), jsonObj.toString()));
            }
        }
        if ((jsonObj.get("apiVersion") != null && !jsonObj.get("apiVersion").isJsonNull()) && !jsonObj.get("apiVersion").isJsonPrimitive()) {
            throw new IllegalArgumentException(String.format("Expected the field `apiVersion` to be a primitive type in the JSON string but got `%s`", jsonObj.get("apiVersion").toString()));
        }
        if ((jsonObj.get("kind") != null && !jsonObj.get("kind").isJsonNull()) && !jsonObj.get("kind").isJsonPrimitive()) {
            throw new IllegalArgumentException(String.format("Expected the field `kind` to be a primitive type in the JSON string but got `%s`", jsonObj.get("kind").toString()));
        }
        // validate the optional field `metadata`
        if (jsonObj.get("metadata") != null && !jsonObj.get("metadata").isJsonNull()) {
//XXX            V1ObjectMeta.validateJsonObject(jsonObj.getAsJsonObject("metadata"));
        }
    }

    public void setVersions(String versions) {
        this.versions = versions;
    }

    public static class CustomTypeAdapterFactory implements TypeAdapterFactory {
        @SuppressWarnings("unchecked")
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!APIGroupDiscovery.class.isAssignableFrom(type.getRawType())) {
                return null; // this class only serializes 'GenericObject' and its subtypes
            }
            final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
            final TypeAdapter<APIGroupDiscovery> thisAdapter
                    = gson.getDelegateAdapter(this, TypeToken.get(APIGroupDiscovery.class));

            return (TypeAdapter<T>) new TypeAdapter<APIGroupDiscovery>() {
                @Override
                public void write(JsonWriter out, APIGroupDiscovery value) throws IOException {
                    JsonObject obj = thisAdapter.toJsonTree(value).getAsJsonObject();
                    elementAdapter.write(out, obj);
                }

                @Override
                public APIGroupDiscovery read(JsonReader in) throws IOException {
                    JsonObject jsonObj = elementAdapter.read(in).getAsJsonObject();
                    validateJsonObject(jsonObj);
                    return thisAdapter.fromJsonTree(jsonObj);
                }

            }.nullSafe();
        }
    }

    /**
     * Create an instance of GenericObject given an JSON string
     *
     * @param jsonString JSON string
     * @return An instance of GenericObject
     * @throws IOException if the JSON string is invalid with respect to GenericObject
     */
    public static APIGroupDiscovery fromJson(String jsonString) throws IOException {
        return new JSON().getGson().fromJson(jsonString, APIGroupDiscovery.class);
    }

    /**
     * Convert an instance of GenericObject to an JSON string
     *
     * @return JSON string
     */
    public String toJson() {
//        return data;
        return new JSON().getGson().toJson(this);
    }
}
