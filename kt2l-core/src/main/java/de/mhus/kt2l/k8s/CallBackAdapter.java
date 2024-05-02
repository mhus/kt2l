package de.mhus.kt2l.k8s;

import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiException;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public class CallBackAdapter<T> implements ApiCallback<T> {
    private final Logger logger;

    public CallBackAdapter(Logger logger) {

        this.logger = logger;
    }

    @Override
    public void onFailure(ApiException e, int i, Map<String, List<String>> map) {
        logger.error("ApiException", e);
    }

    @Override
    public void onSuccess(T t, int i, Map<String, List<String>> map) {
        logger.debug("Success", t);
    }

    @Override
    public void onUploadProgress(long l, long l1, boolean b) {
        logger.debug("onUploadProgress");
    }

    @Override
    public void onDownloadProgress(long l, long l1, boolean b) {
        logger.debug("onDownloadProgress {} {} {}", l, l1, b);
    }
}
