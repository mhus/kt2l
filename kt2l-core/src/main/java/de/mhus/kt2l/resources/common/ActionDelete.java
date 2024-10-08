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
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.aaa.UsersConfiguration.ROLE;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.ViewsConfiguration;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.ui.ProgressDialog;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static de.mhus.commons.tools.MLang.tryThis;

@Component
@Slf4j
@WithRole(ROLE.WRITE)
public class ActionDelete implements ResourceAction {

    @Autowired
    private ViewsConfiguration viewsConfiguration;

    @Autowired
    private K8sService k8s;
//    @Autowired
//    private PodK8s podHandler;

    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return !K8s.CONTAINER.equals(type) && !K8s.NODE.equals(type);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {
        return canHandleType(cluster, type) && selected.size() > 0;
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

        final var config = viewsConfiguration.getConfig("resourcesDelete");
        Checkbox parallelExecution = new Checkbox("Parallel execution");
        parallelExecution.setValue(config.getBoolean("parallel", false));
        dialog.getFooter().add(parallelExecution);

        Button cancelButton = new Button("Cancel", e -> {
            dialog.close();
            context.getUi().remove(dialog);
        });
        dialog.getFooter().add(cancelButton);
        Button deleteButton = new Button("Delete", e -> {
            dialog.close();
            context.getUi().remove(dialog);
            deleteItems(context, parallelExecution.getValue());
        });
        deleteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        dialog.getFooter().add(deleteButton);

        dialog.setWidth(config.getString("width", "500px"));
        dialog.setHeight(config.getString("height", "80%"));

        dialog.open();

    }

    private void deleteItems(ExecutionContext context, boolean parallel) {
        LOGGER.info("Delete resources");

        ProgressDialog dialog = new ProgressDialog();
        dialog.setHeaderTitle("Delete " + context.getSelected().size() + " " + (context.getSelected().size() > 1 ? "Items": "Item") + "?");
        dialog.setMax(context.getSelected().size());
        dialog.open();

        if (parallel) {
            // start all in parallel
            final List<Thread> threads = new LinkedList<>();

            context.getSelected().forEach(res -> {
                final var thread = Thread.startVirtualThread(() -> {
                    try (var sce = context.getSecurityContext().enter()) {
                        var handler = k8s.getTypeHandler(res, context.getCluster(), context.getType());
                        handler.delete(context.getCluster().getApiProvider(), res.getMetadata().getName(), res.getMetadata().getNamespace());
                    } catch (Exception e) {
                        LOGGER.error("delete resource {}", res, e);
                        context.getErrors().add(e);
                    }
                });
                threads.add(thread);
            });
            // wait to finish
            Thread.startVirtualThread(() -> {
                threads.forEach(t -> {
                    try {
                        t.join();
                        context.getUi().access(() -> {
                            dialog.next("");
                        });
                    } catch (InterruptedException e) {
                        LOGGER.error("join", e);
                    }
                });
                context.getUi().access(() -> {
                    dialog.close();
                });
                context.finished();
            });
        } else {
            // start one after the other
            Thread.startVirtualThread(() -> {
                context.getSelected().forEach(res -> {
                    try (var sce = context.getSecurityContext().enter()) {
                        context.getUi().access(() -> {
                            dialog.setProgress(dialog.getProgress() + 1, res.getMetadata().getNamespace() + "." + res.getMetadata().getName());
                        });
                        var handler = k8s.getTypeHandler(res, context.getCluster(), context.getType());
                        handler.delete(context.getCluster().getApiProvider(), res.getMetadata().getName(), res.getMetadata().getNamespace());
                    } catch (Exception e) {
                        LOGGER.error("delete resource {}", res, e);
                        context.getErrors().add(e);
                    }
                });
                context.getUi().access(() -> {
                    dialog.close();
                });
                context.finished();
            });
        }
    }

    @Override
    public String getTitle() {
        return "Delete";
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
        return "CONTROL+BACKSPACE";
    }

    @Override
    public String getDescription() {
        return "Delete pods or container";
    }

    @Override
    public AbstractIcon getIcon() {
        return VaadinIcon.FILE_REMOVE.create();
    }

    private String getColumnValue(KubernetesObject v) {
        var name = v.getMetadata().getName();
        var ns = v.getMetadata().getNamespace();
        var kind = tryThis(() -> v.getKind() ).orElse(null);
        if (kind == null)
            kind = v.getClass().getSimpleName();
//        if (v instanceof ContainerResource container) {
//            name = name + "." + container.getContainerName();
//        }
        return kind + ": " + (ns == null ? "" : ns + ".") + (name == null ? v.toString() : name);
    }

}
