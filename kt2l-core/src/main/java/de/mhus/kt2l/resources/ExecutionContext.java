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
import de.mhus.kt2l.cluster.ClusterConfiguration;
import de.mhus.kt2l.core.MainView;
import de.mhus.kt2l.core.XTab;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Getter
@Builder
public class ExecutionContext {
    private UI ui;
    private String resourceType;
    private Set<? extends KubernetesObject> selected;
    private CoreV1Api api;
    private String namespace;
    private ClusterConfiguration.Cluster clusterConfiguration;
    @Setter
    private boolean needGridRefresh;
    private List<Exception> errors = new LinkedList<>();
    private ResourcesGrid grid;
    private MainView mainView;
    private XTab selectedTab;

    public void finished() {
        if (errors.size() > 1) {
            Notification notification = Notification
                    .show("Error\n"+errors); //XXX
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } else {
            Notification notification = Notification
                    .show("Completed!");
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            if (isNeedGridRefresh()) {
                grid.refresh(0);
            }

        }
    }

}
