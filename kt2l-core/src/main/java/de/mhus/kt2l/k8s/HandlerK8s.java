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

import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import okhttp3.Call;

public interface HandlerK8s {

    K8s getManagedResourceType();

    String getDescribe(ApiProvider apiProvider, KubernetesObject res);

    void replace(ApiProvider apiProvider, String name, String namespace, String yaml) throws ApiException;

    Object delete(ApiProvider apiProvider, String name, String namespace) throws ApiException;

    Object create(ApiProvider apiProvider, String yaml) throws ApiException;

    <L extends KubernetesListObject> L createResourceListWithoutNamespace(ApiProvider apiProvider) throws ApiException;

    <L extends KubernetesListObject> L createResourceListWithNamespace(ApiProvider apiProvider, String namespace) throws ApiException;

    Call createResourceWatchCall(ApiProvider apiProvider) throws ApiException;

}
