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

import de.mhus.commons.lang.IRegistry;
import de.mhus.commons.tools.MObject;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.resources.clusterrole.ClusterRoleWatch;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.util.Watch;

import java.io.IOException;

public abstract class ClusterBackgroundJob {

    public static synchronized <W extends ClusterBackgroundJob> W instance(Core core, Cluster cluster, Class<W> clazz) {
        return (W)core.getBackgroundJob(cluster.getName(), clazz, () -> MObject.newInstance(clazz));
    }

    protected ClusterBackgroundJob() {
    }

    public abstract void close();

    public abstract void init(Core core, String clusterId, String jobId) throws IOException;

    public abstract <V extends KubernetesObject> IRegistry<Watch.Response<V>> getEventHandler();

}
