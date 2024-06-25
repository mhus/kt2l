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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.WithRole;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

import static de.mhus.commons.tools.MLang.tryThis;

@Component
@Slf4j
@WithRole(UsersConfiguration.ROLE.READ)
public class DescribeAction implements ResourceAction {
    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return true;
    }

    @Autowired
    private K8sService k8sService;

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {
        return selected.size() > 0;
    }

    @Override
    public void execute(ExecutionContext context) {
        showPreview(context.getCore(), context.getCluster(), context.getType(), context.getSelected());
    }

    public void showPreview(Core core, Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {

        // prepare preview
        StringBuilder sb = new StringBuilder();
        selected.forEach(
                res -> {
                    final var handler = k8sService.getTypeHandler(type /*K8sUtil.toResource(res, cluster) */);
                    final var previewText = handler.getDescribe(cluster.getApiProvider(), res);
                    sb.append(">>> ")
                            .append( tryThis(() -> res.getKind()).or(null) == null ? type.getKind() : res.getKind())
                            .append(" ").append(tryThis(() -> res.getMetadata().getName()).or("?")).append('\n');
                    sb.append(previewText).append('\n');
                }
        );

        Dialog dialog = new Dialog();
        dialog.getHeader().add(VaadinIcon.FILE_PRESENTATION.create());
        dialog.setHeaderTitle("Describe Resource");
        var preview = new TextArea();
        preview.addClassName("preview");
        preview.addClassName("no-word-wrap");
        preview.setReadOnly(true);
        preview.setSizeFull();
        preview.setValue(sb.toString());
        dialog.add(preview);
        dialog.setWidth("80%");
        dialog.setHeight("80%");
        final var ui = core.ui();
        Button closeButton = new Button("Close", e -> {
            dialog.close();
            ui.remove(dialog);
        });
        dialog.getFooter().add(closeButton);
        dialog.open();
    }

    @Override
    public String getTitle() {
        return "Describe;icon=" + VaadinIcon.FILE_PRESENTATION;
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.VIEW_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.VIEW_ORDER+100;
    }

    @Override
    public String getShortcutKey() {
        return "D";
    }

    @Override
    public String getDescription() {
        return "Describe Resource";
    }
}
