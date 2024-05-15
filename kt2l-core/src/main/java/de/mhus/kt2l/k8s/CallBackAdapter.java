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
        logger.warn("♪ ApiException", e);
    }

    @Override
    public void onSuccess(T t, int i, Map<String, List<String>> map) {
        logger.debug("♪ Success", t);
    }

    @Override
    public void onUploadProgress(long l, long l1, boolean b) {
        logger.debug("♪ onUploadProgress");
    }

    @Override
    public void onDownloadProgress(long l, long l1, boolean b) {
        logger.trace("♪ onDownloadProgress {} {} {}", l, l1, b);
    }
}
