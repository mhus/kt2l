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

package de.mhus.kt2l.ai;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.config.UsersConfiguration.ROLE;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.core.WithRole;
import io.kubernetes.client.common.KubernetesObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@WithRole(ROLE.READ)
public class AiAction implements ResourceAction  {

    @Autowired
    private PanelService panelService;

    @Autowired
    private AiConfiguration aiConfiguration;

    @Override
    public boolean canHandleResourceType(String resourceType) {
        return aiConfiguration.isEnabled();
    }

    @Override
    public boolean canHandleResource(String resourceType, Set<? extends KubernetesObject> selected) {
        return selected.size() > 0;
    }

    @Override
    public void execute(ExecutionContext context) {

        List<KubernetesObject> resources = new LinkedList<>();
        for (var selected : context.getSelected()) {
            resources.add(selected);
        }

        if (resources.size() == 0) return;

        // process
        var name = resources.getFirst().getMetadata().getName();
        panelService.addPanel(
                context.getSelectedTab(),
                context.getClusterConfiguration().name() + ":" + context.getResourceType() + ":" + name + ":ai",
                name,
                false,
                VaadinIcon.ACADEMY_CAP.create(),
                () -> new AiResourcePanel(resources, context)
                ).setHelpContext("ai").select();
    }

    @Override
    public String getTitle() {
        return "AI;icon=" + VaadinIcon.CROSSHAIRS;
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.TOOLS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.TOOLS_ORDER + 10;
    }

    @Override
    public String getShortcutKey() {
        return "a";
    }

    @Override
    public String getDescription() {
        return "Analyse with AI";
    }


}
