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

package de.mhus.kt2l.resources.deployment;

import com.google.gson.reflect.TypeToken;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.util.AbstractClusterWatch;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Configurable;

import java.lang.reflect.Type;

@Slf4j
@Configurable
public class DeploymentWatch extends AbstractClusterWatch<V1Deployment> {

    @Override
    public V1APIResource getManagedType() {
        return K8s.DEPLOYMENT;
    }

    @Override
    protected Type createTypeToken() {
        return new TypeToken<Watch.Response<V1Deployment>>() {}.getType();
    }

}
