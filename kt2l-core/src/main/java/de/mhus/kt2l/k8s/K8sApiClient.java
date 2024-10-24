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

import de.mhus.kt2l.generated.DeployInfo;
import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.ApiResponse;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.Pair;
import io.kubernetes.client.openapi.ServerConfiguration;
import io.kubernetes.client.openapi.auth.Authentication;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.net.ssl.KeyManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.text.DateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Proxy original ApiClient to handle exceptions if the key is outtimed and retry the request.
 *
 */

/*
2024-06-18 11:08:57,684 WARN  Failed to execute call Request{method=GET,
    url=https://127.0.0.1:6443/apis/metrics.k8s.io/v1beta1/namespaces/default/pods?watch=false,
    headers=[Accept:application/json, Content-Type:application/json,
    User-Agent:Kubernetes Java Client/20.0.1-legacy-SNAPHOST kt2l-core/0.0.1-SNAPSHOT]}
    with 503 and service unavailable
2024-06-18 13:18:49,927 WARN  Failed to execute call Request{method=GET,
    url=https://2d7353426269f291d1c06c5203a0ff32.gr7.eu-central-1.eks.amazonaws.com/api/v1/namespaces,
    headers=[authorization:Bearer k8s-aws-v1., Accept:application/json, Content-Type:application/json,
    User-Agent:Kubernetes Java Client/20.0.1-legacy-SNAPHOST kt2l-core/0.0.1-SNAPSHOT],
    tags={class java.lang.Object=de.mhus.kt2l.k8s.K8sUtil$1@299ce708}}
    with 401 and {"kind":"Status","apiVersion":"v1","metadata":{},"status":"Failure","message":"Unauthorized",
    "reason":"Unauthorized","code":401}

    503 is only thrown if a service is not available, e.g. metrics server is not running
    401 is thrown if the token is not valid anymore, in this case the client should refresh the token and retry the request

 */

@Slf4j
public class K8sApiClient extends ApiClient {

    private final ApiProvider apiProvider;

    @Override
    public Call buildCall(String baseUrl, String path, String method, List<Pair> queryParams, List<Pair> collectionQueryParams, Object body, Map<String, String> headerParams, Map<String, String> cookieParams, Map<String, Object> formParams, String[] authNames, ApiCallback callback) throws ApiException {
        return client.buildCall(baseUrl, path, method, queryParams, collectionQueryParams, body, headerParams, cookieParams, formParams, authNames, callback);
    }

    @Override
    public Request buildRequest(String baseUrl, String path, String method, List<Pair> queryParams, List<Pair> collectionQueryParams, Object body, Map<String, String> headerParams, Map<String, String> cookieParams, Map<String, Object> formParams, String[] authNames, ApiCallback callback) throws ApiException {
        return client.buildRequest(baseUrl, path, method, queryParams, collectionQueryParams, body, headerParams, cookieParams, formParams, authNames, callback);
    }

    @Override
    public String buildUrl(String baseUrl, String path, List<Pair> queryParams, List<Pair> collectionQueryParams) {
        return client.buildUrl(baseUrl, path, queryParams, collectionQueryParams);
    }

    @Override
    public Integer getServerIndex() {
        return client.getServerIndex();
    }

    @Override
    public List<ServerConfiguration> getServers() {
        return client.getServers();
    }

    @Override
    public Map<String, String> getServerVariables() {
        return client.getServerVariables();
    }

    @Override
    public void setAWS4Configuration(String accessKey, String secretKey, String region, String service) {
        client.setAWS4Configuration(accessKey, secretKey, region, service);
    }

    @Override
    public ApiClient setServerIndex(Integer serverIndex) {
        return client.setServerIndex(serverIndex);
    }

    @Override
    public ApiClient setServers(List<ServerConfiguration> servers) {
        return client.setServers(servers);
    }

    @Override
    public ApiClient setServerVariables(Map<String, String> serverVariables) {
        return client.setServerVariables(serverVariables);
    }

    @Override
    public void updateParamsForAuth(String[] authNames, List<Pair> queryParams, Map<String, String> headerParams, Map<String, String> cookieParams, String payload, String method, URI uri) throws ApiException {
        client.updateParamsForAuth(authNames, queryParams, headerParams, cookieParams, payload, method, uri);
    }

    private ApiClient client;
    private String userAgent;

    public K8sApiClient(ApiProvider apiProvider) {
        this.apiProvider = apiProvider;
        client = apiProvider.getApiClient();
        setUserAgent(userAgent + " kt2l-core/" + DeployInfo.VERSION);
    }

    private boolean canRetry(ApiException e) {
        return e.getCode() == 401;
    }

    private void refreshClient() {
        LOGGER.debug("Refreshing api client");
        apiProvider.invalidate();
        client = apiProvider.getApiClient();
    }

    private String extractMessage(String responseBody) {
        if (responseBody == null) return null;
        if (responseBody.startsWith("{") && responseBody.endsWith("}")) {
            var pos = responseBody.indexOf("\"message\":\"");
            if (pos >= 0) {
                pos += 10;
                var end = responseBody.indexOf("\"", pos);
                if (end >= 0) {
                    return responseBody.substring(pos, end);
                }
            }
        }
        return responseBody;
    }

    @Override
    public ApiClient addDefaultCookie(String key, String value) {
        return client.addDefaultCookie(key, value);
    }

    @Override
    public ApiClient addDefaultHeader(String key, String value) {
        return client.addDefaultHeader(key, value);
    }

    @Override
    public RequestBody buildRequestBodyFormEncoding(Map<String, Object> formParams) {
        return client.buildRequestBodyFormEncoding(formParams);
    }

    @Override
    public RequestBody buildRequestBodyMultipart(Map<String, Object> formParams) {
        return client.buildRequestBodyMultipart(formParams);
    }

    @Override
    public String collectionPathParameterToString(String collectionFormat, Collection value) {
        return client.collectionPathParameterToString(collectionFormat, value);
    }

    @Override
    public <T> T deserialize(Response response, Type returnType) throws ApiException {
        return client.deserialize(response, returnType);
    }

    @Override
    public File downloadFileFromResponse(Response response) throws ApiException {
        return client.downloadFileFromResponse(response);
    }

    @Override
    public String escapeString(String str) {
        return client.escapeString(str);
    }

    @Override
    public <T> ApiResponse<T> execute(Call call) throws ApiException {
        try {
            return client.execute(call);
        } catch (ApiException e) {
            LOGGER.warn("Failed to execute call {} with {} and {}",call.request(),e.getCode(), extractMessage(e.getResponseBody()), e);
            if (canRetry(e)) {
                refreshClient();
                return client.execute(call);
            }
            throw e;
        }
    }

    @Override
    public <T> ApiResponse<T> execute(Call call, Type returnType) throws ApiException {
        try {
            return client.execute(call, returnType);
        } catch (ApiException e) {
            if (e.getCode() == 404) { // 404 is fine, do not log stacktrace
                LOGGER.debug("Failed to execute call {} with {} and {}",call.request(),e.getCode(), extractMessage(e.getResponseBody()));
                throw e;
            }
            LOGGER.warn("Failed to execute call {} with {} and {}",call.request(),e.getCode(), extractMessage(e.getResponseBody()), e);
            if (canRetry(e)) { // check if retry is possible
                refreshClient();
                return client.execute(call, returnType);
            }
            throw e;
        }
    }

    @Override
    public <T> void executeAsync(Call call, ApiCallback<T> callback) {
        client.executeAsync(call, new ApiCallback<T>() {

            @Override
            public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
                LOGGER.warn("Failed to execute call {} with {} and {}",call.request(),e.getCode(), extractMessage(e.getResponseBody()), e);
                if (canRetry(e)) {
                    refreshClient();
                    client.executeAsync(call, callback);
                }
                callback.onFailure(e, statusCode, responseHeaders);
            }

            @Override
            public void onSuccess(T result, int statusCode, Map<String, List<String>> responseHeaders) {
                callback.onSuccess(result, statusCode, responseHeaders);
            }

            @Override
            public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
                callback.onUploadProgress(bytesWritten, contentLength, done);
            }

            @Override
            public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
                callback.onDownloadProgress(bytesRead, contentLength, done);
            }
        });
    }

    @Override
    public <T> void executeAsync(Call call, Type returnType, ApiCallback<T> callback) {
        client.executeAsync(call, returnType , new ApiCallback<T>() {

            @Override
            public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
                LOGGER.warn("Failed to execute call {} with {} and {}",call.request(),e.getCode(), extractMessage(e.getResponseBody()), e);
                if (canRetry(e)) {
                    refreshClient();
                    client.executeAsync(call, returnType, callback);
                }
                callback.onFailure(e, statusCode, responseHeaders);
            }

            @Override
            public void onSuccess(T result, int statusCode, Map<String, List<String>> responseHeaders) {
                callback.onSuccess(result, statusCode, responseHeaders);
            }

            @Override
            public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
                callback.onUploadProgress(bytesWritten, contentLength, done);
            }

            @Override
            public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
                callback.onDownloadProgress(bytesRead, contentLength, done);
            }
        });
    }

    @Override
    public Authentication getAuthentication(String authName) {
        return client.getAuthentication(authName);
    }

    @Override
    public Map<String, Authentication> getAuthentications() {
        return client.getAuthentications();
    }

    @Override
    public String getBasePath() {
        return client.getBasePath();
    }

    @Override
    public int getConnectTimeout() {
        return client.getConnectTimeout();
    }

    @Override
    public DateFormat getDateFormat() {
        return client.getDateFormat();
    }

    @Override
    public OkHttpClient getHttpClient() {
        return client.getHttpClient();
    }

    @Override
    public JSON getJSON() {
        return client.getJSON();
    }

    @Override
    public KeyManager[] getKeyManagers() {
        return client.getKeyManagers();
    }

    @Override
    public int getReadTimeout() {
        return client.getReadTimeout();
    }

    @Override
    public InputStream getSslCaCert() {
        return client.getSslCaCert();
    }

    @Override
    public String getTempFolderPath() {
        return client.getTempFolderPath();
    }

    @Override
    public int getWriteTimeout() {
        return client.getWriteTimeout();
    }

    @Override
    public String guessContentTypeFromFile(File file) {
        return client.guessContentTypeFromFile(file);
    }

    @Override
    public <T> T handleResponse(Response response, Type returnType) throws ApiException {
        return client.handleResponse(response, returnType);
    }

    @Override
    public boolean isDebugging() {
        return client.isDebugging();
    }

    @Override
    public boolean isJsonMime(String mime) {
        return client.isJsonMime(mime);
    }

    @Override
    public boolean isVerifyingSsl() {
        return client.isVerifyingSsl();
    }

    @Override
    public List<Pair> parameterToPair(String name, Object value) {
        return client.parameterToPair(name, value);
    }

    @Override
    public List<Pair> parameterToPairs(String collectionFormat, String name, Collection value) {
        return client.parameterToPairs(collectionFormat, name, value);
    }

    @Override
    public String parameterToString(Object param) {
        return client.parameterToString(param);
    }

    @Override
    public File prepareDownloadFile(Response response) throws IOException {
        return client.prepareDownloadFile(response);
    }

    @Override
    public void processCookieParams(Map<String, String> cookieParams, Request.Builder reqBuilder) {
        client.processCookieParams(cookieParams, reqBuilder);
    }

    @Override
    public void processHeaderParams(Map<String, String> headerParams, Request.Builder reqBuilder) {
        client.processHeaderParams(headerParams, reqBuilder);
    }

    @Override
    public String sanitizeFilename(String filename) {
        return client.sanitizeFilename(filename);
    }

    @Override
    public String selectHeaderAccept(String[] accepts) {
        return client.selectHeaderAccept(accepts);
    }

    @Override
    public String selectHeaderContentType(String[] contentTypes) {
        return client.selectHeaderContentType(contentTypes);
    }

    @Override
    public RequestBody serialize(Object obj, String contentType) throws ApiException {
        return client.serialize(obj, contentType);
    }

    @Override
    public void setAccessToken(String accessToken) {
        client.setAccessToken(accessToken);
    }

    @Override
    public void setApiKey(String apiKey) {
        client.setApiKey(apiKey);
    }

    @Override
    public void setApiKeyPrefix(String apiKeyPrefix) {
        client.setApiKeyPrefix(apiKeyPrefix);
    }

    @Override
    public ApiClient setBasePath(String basePath) {
        return client.setBasePath(basePath);
    }

    @Override
    public ApiClient setConnectTimeout(int connectionTimeout) {
        return client.setConnectTimeout(connectionTimeout);
    }

    @Override
    public ApiClient setDateFormat(DateFormat dateFormat) {
        return client.setDateFormat(dateFormat);
    }

    @Override
    public ApiClient setDebugging(boolean debugging) {
        return client.setDebugging(debugging);
    }

    @Override
    public ApiClient setHttpClient(OkHttpClient newHttpClient) {
        return client.setHttpClient(newHttpClient);
    }

    @Override
    public ApiClient setJSON(JSON json) {
        return client.setJSON(json);
    }

    @Override
    public ApiClient setKeyManagers(KeyManager[] managers) {
        return client.setKeyManagers(managers);
    }

    @Override
    public ApiClient setLenientOnJson(boolean lenientOnJson) {
        return client.setLenientOnJson(lenientOnJson);
    }

    @Override
    public ApiClient setLocalDateFormat(DateTimeFormatter dateFormat) {
        return client.setLocalDateFormat(dateFormat);
    }

    @Override
    public ApiClient setOffsetDateTimeFormat(DateTimeFormatter dateFormat) {
        return client.setOffsetDateTimeFormat(dateFormat);
    }

    @Override
    public void setPassword(String password) {
        client.setPassword(password);
    }

    @Override
    public ApiClient setReadTimeout(int readTimeout) {
        return client.setReadTimeout(readTimeout);
    }

    @Override
    public ApiClient setSqlDateFormat(DateFormat dateFormat) {
        return client.setSqlDateFormat(dateFormat);
    }

    @Override
    public ApiClient setSslCaCert(InputStream sslCaCert) {
        return client.setSslCaCert(sslCaCert);
    }

    @Override
    public ApiClient setTempFolderPath(String tempFolderPath) {
        return client.setTempFolderPath(tempFolderPath);
    }

    @Override
    public ApiClient setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        if (client == null) return null;
        return client.setUserAgent(userAgent);
    }

    @Override
    public void setUsername(String username) {
        client.setUsername(username);
    }

    @Override
    public ApiClient setVerifyingSsl(boolean verifyingSsl) {
        return client.setVerifyingSsl(verifyingSsl);
    }

    @Override
    public ApiClient setWriteTimeout(int writeTimeout) {
        return client.setWriteTimeout(writeTimeout);
    }

    public void setClient(ApiClient client) {
        this.client = client;
    }
}
