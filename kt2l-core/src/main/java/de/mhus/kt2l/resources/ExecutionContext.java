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
import de.mhus.kt2l.aaa.SecurityContext;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.Builder;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Getter
@Builder
public class ExecutionContext {
    private UI ui;
    private V1APIResource type;
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
            if (errors.size() == 1) {
                UiUtil.showErrorNotification("Error in Action", errors.get(0));
            } else
            if (errors.size() > 0) {
                UiUtil.showErrorNotification("Error in Action\n" + errors, errors.get(0));
            } else {
                UiUtil.showSuccessNotification("Completed Action!");
            }
        });
    }

}
