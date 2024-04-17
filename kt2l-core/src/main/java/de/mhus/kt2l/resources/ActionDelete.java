/**
 * This file is part of kt2l-core.
 *
 * kt2l-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * kt2l-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with kt2l-core.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l.resources;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.config.UsersConfiguration.ROLE;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.core.WithRole;
import io.kubernetes.client.common.KubernetesObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Set;

@Component
@Slf4j
@WithRole(ROLE.WRITE)
public class ActionDelete implements ResourceAction {

    @Autowired
    private K8sService k8s;

    @Override
    public boolean canHandleResourceType(String resourceType) {
        return true;
    }

    @Override
    public boolean canHandleResource(String resourceType, Set<? extends KubernetesObject> selected) {
        return selected.size() > 0;
    }

    @Override
    public void execute(ExecutionContext context) {

        Grid<KubernetesObject> grid = new Grid<>();
        grid.setSizeFull();
        grid.addColumn(v -> getColumnValue(v)).setHeader("Name");
        grid.setItems(new LinkedList<>(context.getSelected()));

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Delete " + context.getSelected().size() + " " + (context.getSelected().size() > 1 ? "Items": "Item") + "?");
        dialog.add(grid);

        Button cancelButton = new Button("Cancel", e -> {
            dialog.close();
            context.getUi().remove(dialog);
        });
        dialog.getFooter().add(cancelButton);
        Button deleteButton = new Button("Delete", e -> {
            dialog.close();
            context.getUi().remove(dialog);
            deleteItems(context);
        });
        deleteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        dialog.getFooter().add(deleteButton);

        dialog.setWidth("500px");
        dialog.setHeight("80%");

        dialog.open();

    }

    private void deleteItems(ExecutionContext context) {
        context.setNeedGridRefresh(true);
        LOGGER.info("Delete pod");
        context.getSelected().forEach(o -> {
            try {
                var handler = k8s.getResourceHandler(o.getKind());
                handler.delete(context.getApi(), o.getMetadata().getName(), o.getMetadata().getNamespace());
            } catch (Exception e) {
                LOGGER.error("delete resource {}", o, e);
                context.getErrors().add(e);
            }
        });
        context.finished();
    }

    @Override
    public String getTitle() {
        return "Delete;icon=" + VaadinIcon.FILE_REMOVE;
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.ACTIONS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.ACTIONS_ORDER + 100;
    }

    @Override
    public String getShortcutKey() {
        return "CONTROL+DELETE";
    }

    @Override
    public String getDescription() {
        return "Delete pods or container";
    }

    private String getColumnValue(KubernetesObject v) {
        var name = v.getMetadata().getName();
        var ns = v.getMetadata().getNamespace();
        var kind = v.getKind();
        if (kind == null)
            kind = v.getClass().getSimpleName();
        return kind + ": " + (ns == null ? "" : ns + ".") + (name == null ? v.toString() : name);
    }

}
