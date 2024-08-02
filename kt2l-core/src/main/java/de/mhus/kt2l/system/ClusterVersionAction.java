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
package de.mhus.kt2l.system;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterAction;
import de.mhus.kt2l.core.Core;
import io.kubernetes.client.extended.kubectl.Kubectl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@WithRole(UsersConfiguration.ROLE.READ)
@Slf4j
public class ClusterVersionAction implements ClusterAction {
    @Override
    public boolean canHandle(Core core) {
        return true;
    }

    @Override
    public boolean canHandle(Core core, Cluster cluster) {
        return true;
    }

    @Override
    public String getTitle() {
        return "Version";
    }

    @Override
    public void execute(Core core, Cluster cluster) {
        try {
            var version = Kubectl.version().apiClient(cluster.getApiProvider().getClient()).execute();
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Kubernetes Cluster Version");
            TextArea text = new TextArea();
            text.setReadOnly(true);
            text.setValue(version.toString());
            text.setSizeFull();
            dialog.setText(text);
            dialog.setConfirmText("Close");
            dialog.setWidth("800px");
            dialog.open();
        } catch (Exception e) {
        }
    }

    @Override
    public AbstractIcon getIcon() {
        return VaadinIcon.INFO_CIRCLE_O.create();
    }

    @Override
    public int getPriority() {
        return 11111;
    }
}
