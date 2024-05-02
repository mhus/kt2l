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

package de.mhus.kt2l.resources.pod;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;

public class ContainerResource implements KubernetesObject {

    private final PodGrid.Container container;

    public ContainerResource(PodGrid.Container container) {
        this.container = container;
    }

    @Override
    public V1ObjectMeta getMetadata() {
        return container.getPod().getMetadata();
    }

    @Override
    public String getApiVersion() {
        return container.getPod().getApiVersion();
    }

    @Override
    public String getKind() {
        return container.getPod().getKind();
    }

    public String getContainerName() {
        return container.getName();
    }


    public V1Pod getPod() {
        return container.getPod();
    }
}
