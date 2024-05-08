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
package de.mhus.kt2l.cluster;

import de.mhus.commons.tools.MPeriod;
import de.mhus.commons.tree.ITreeNode;
import de.mhus.kt2l.core.UiUtil;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sService;
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static de.mhus.commons.tools.MLang.tryThis;


@Slf4j
@Getter
public class Cluster {
    private final String user;
    private final String name;
    private final String title;
    private final boolean enabled;
    private final String defaultNamespace;
    private final K8s.RESOURCE defaultResourceType;
    private final UiUtil.COLOR color;
    private final ITreeNode config;
    private final long apiProviderTimeout;
    @Setter
    private List<String> currentNamespaces;
    @Setter
    private List<V1APIResource> resourceTypes;

    private K8sService k8sService;
    private ApiProvider apiProvider;

    Cluster(ClusterConfiguration cc, String user, String name, ITreeNode config) {
        this.user = user;
        this.name = name;
        this.title = config.getString("title", name);
        this.enabled = config.getBoolean("enabled", true);
        this.defaultNamespace = config.getString("defaultNamespace", cc.defaultNamespace());
        this.defaultResourceType = K8s.toResourceType(config.getString("defaultResourceType", cc.defaultResourceType()));
        this.color = UiUtil.toColor(config.getString("color", null));
        this.config = config;
        this.apiProviderTimeout = MPeriod.parseInterval(config.getString( "apiProviderTimeout"), ApiProvider.DEFAULT_TIMEOUT);
    }

    void setK8sService(K8sService k8sService) {
        this.k8sService = k8sService;
    }

    public ApiProvider getApiProvider() {
        if (apiProvider ==  null)
            apiProvider =  tryThis(() -> k8sService.getKubeClient(name, apiProviderTimeout)).onFailure(
                    e -> LOGGER.warn("Can't get apiProvider for cluster " + name, e)
            ).get();
        return apiProvider;
    }

    

}
