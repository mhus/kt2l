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
package de.mhus.kt2l.events;

import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import io.kubernetes.client.common.KubernetesObject;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class EventResourceAction implements ResourceAction {
    @Override
    public boolean canHandleResourceType(K8s resourceType) {
        return false;
    }

    @Override
    public boolean canHandleResource(K8s resourceType, Set<? extends KubernetesObject> selected) {
        return false;
    }

    @Override
    public void execute(ExecutionContext context) {

    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    public String getMenuPath() {
        return "";
    }

    @Override
    public int getMenuOrder() {
        return 0;
    }

    @Override
    public String getShortcutKey() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }
}
