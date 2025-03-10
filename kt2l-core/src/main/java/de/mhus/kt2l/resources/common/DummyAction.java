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
package de.mhus.kt2l.resources.common;

import com.vaadin.flow.component.icon.AbstractIcon;
import de.mhus.commons.tools.MSystem;
import de.mhus.commons.tools.MThread;
import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.ui.BackgroundJobDialog;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
@WithRole(UsersConfiguration.ROLE.WRITE)
public class DummyAction implements ResourceAction  {
    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return MSystem.isVmDebug();
    }

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {
        return true;
    }

    @Override
    public void execute(ExecutionContext context) {
        BackgroundJobDialog dialog = new BackgroundJobDialog(context.getCore(), context.getCluster(), true);
        dialog.setHeaderTitle("Dummy");
        dialog.open();
       dialog.setMax(100);

        Thread.startVirtualThread(() -> {
            for (int i = 0; i < 100; i++) {
                if (dialog.isCanceled())
                    break;
                final int step = i;
                LOGGER.info("{} Step {}", MSystem.getObjectId(DummyAction.this), step);
                context.getUi().access(() -> dialog.next("Step " + step));
                MThread.sleep(1000);
            }
            context.getUi().access(() -> dialog.close());
            context.finished();
       });

    }

    @Override
    public String getTitle() {
        return "Dummy";
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.ACTIONS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.ACTIONS_ORDER + 200;
    }

    @Override
    public String getShortcutKey() {
        return "META+X";
    }

    @Override
    public String getDescription() {
        return "Dummy dummy dummy";
    }

    @Override
    public AbstractIcon getIcon() {
        return null;
    }
}
