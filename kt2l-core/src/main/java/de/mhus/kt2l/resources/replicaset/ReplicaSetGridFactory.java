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

package de.mhus.kt2l.resources.replicaset;

import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ResourceGridFactory;
import de.mhus.kt2l.resources.ResourcesGrid;
import io.kubernetes.client.openapi.models.V1APIResource;
import org.springframework.stereotype.Component;

@Component
@WithRole(UsersConfiguration.ROLE.READ)
public class ReplicaSetGridFactory implements ResourceGridFactory {
    @Override
    public boolean canHandleType(V1APIResource type) {
        return K8s.REPLICA_SET.equals(type);
    }

    @Override
    public ResourcesGrid create(V1APIResource type) {
        return new ReplicaSetGrid();
    }

}
