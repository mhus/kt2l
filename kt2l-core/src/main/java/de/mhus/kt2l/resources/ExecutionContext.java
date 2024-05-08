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

package de.mhus.kt2l.resources;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.SecurityContext;
import de.mhus.kt2l.k8s.K8s;
import io.kubernetes.client.common.KubernetesObject;
import lombok.Builder;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Getter
@Builder
public class ExecutionContext {
    private UI ui;
    private K8s.RESOURCE resourceType;
    private Set<? extends KubernetesObject> selected;
    private String namespace;
    private Cluster cluster;
//    @Setter
//    private boolean needGridRefresh;
    private final List<Exception> errors = new LinkedList<>();
    private ResourcesGrid grid;
    private Core core;
    private DeskTab selectedTab;
    private final SecurityContext securityContext = SecurityContext.create();

    public void finished() {
        getUi().access(() -> {
            if (errors.size() > 0) {
                Notification notification = Notification
                        .show("Error\n"+errors); //XXX
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } else {
                Notification notification = Notification
                        .show("Completed!");
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

//                if (isNeedGridRefresh()) {
//                    grid.refresh(0);
//                }
            }
        });
    }

}
