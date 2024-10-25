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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import de.mhus.commons.tools.MPeriod;
import de.mhus.commons.tools.MThread;
import de.mhus.commons.tree.ITreeNode;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.ui.BackgroundJobDialog;
import de.mhus.kt2l.ui.ProgressDialog;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

import static de.mhus.commons.tools.MLang.tryThis;

@Slf4j
public class ActionDeleteDialog {
    private final ExecutionContext context;
    private final ITreeNode config;
    private final K8sService k8s;
    private Dialog dialog;
    private IntegerField sleepBetweenInMs;
    private Checkbox parallelExecution;
    private Checkbox waitForDisappear;
    private IntegerField waitDisappearTimeoutInSec;
    private boolean canceled = false;

    public ActionDeleteDialog(ExecutionContext context, ITreeNode config, K8sService k8s) {
        this.context = context;
        this.config = config;
        this.k8s = k8s;
        createUI();
    }

    public void open() {
        dialog.open();
    }

    private void createUI() {

        Grid<KubernetesObject> grid = new Grid<>();
        grid.setSizeFull();
        grid.addColumn(v -> getColumnValue(v)).setHeader("Name");
        grid.setItems(new LinkedList<>(context.getSelected()));

        dialog = new Dialog();
        dialog.setHeaderTitle("Delete " + context.getSelected().size() + " " + (context.getSelected().size() > 1 ? "Items": "Item") + "?");
        dialog.add(grid);

        // create options dialog
        final Dialog optionsDialog = createOptionsDialog();

        var optionsDialogButton = new Button("Options", e -> optionsDialog.open() );
        optionsDialogButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        optionsDialogButton.getStyle().set("margin-inline-end", "auto");
        dialog.getFooter().add(optionsDialogButton);

        Button cancelButton = new Button("Cancel", e -> {
            dialog.close();
            context.getUi().remove(dialog);
        });
        dialog.getFooter().add(cancelButton);
        Button deleteButton = new Button("Delete", e -> {
            dialog.close();
            context.getUi().remove(dialog);
            deleteItems();
        });
        deleteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        dialog.getFooter().add(deleteButton);

        dialog.setWidth(config.getString("width", "500px"));
        dialog.setHeight(config.getString("height", "80%"));

    }

    private Dialog createOptionsDialog() {
        final Dialog optionsDialog = new Dialog();
        optionsDialog.setWidth("400px");
        optionsDialog.setHeight("500px");
        optionsDialog.setHeaderTitle("Delete Options");
        VerticalLayout optionsPanel = new VerticalLayout();
        optionsPanel.setSizeFull();
        optionsDialog.add(optionsPanel);

        parallelExecution = new Checkbox("Parallel execution");
        parallelExecution.setValue(config.getBoolean("parallel", false));
        parallelExecution.setWidthFull();
        optionsPanel.add(parallelExecution);

        sleepBetweenInMs = new IntegerField();
        sleepBetweenInMs.setLabel("Sleep between in ms");
        sleepBetweenInMs.setMin(0);
        sleepBetweenInMs.setMax(1000 * 60 * 5);
        sleepBetweenInMs.setStep(100);
        sleepBetweenInMs.setStepButtonsVisible(true);
        sleepBetweenInMs.setValue(config.getInt("sleepMilliseconds", 0));
        sleepBetweenInMs.setWidthFull();
        optionsPanel.add(sleepBetweenInMs);

        waitForDisappear = new Checkbox("Wait for disappear");
        waitForDisappear.setValue(config.getBoolean("waitForDisappear", false));
        waitForDisappear.setWidthFull();
        optionsPanel.add(waitForDisappear);

        waitDisappearTimeoutInSec = new IntegerField();
        waitDisappearTimeoutInSec.setLabel("Wait for disappear timeout in seconds");
        waitDisappearTimeoutInSec.setMin(0);
        waitDisappearTimeoutInSec.setMax(60 * 2);
        waitDisappearTimeoutInSec.setStep(1);
        waitDisappearTimeoutInSec.setStepButtonsVisible(true);
        waitDisappearTimeoutInSec.setValue(config.getInt("disappearTimeoutSeconds", 10));
        waitDisappearTimeoutInSec.setWidthFull();
        optionsPanel.add(waitDisappearTimeoutInSec);


        optionsDialog.getFooter().add(new Button("OK", e -> optionsDialog.close()));
        return optionsDialog;
    }

    private void deleteItems() {
        LOGGER.info("Delete resources");

        BackgroundJobDialog dialog = new BackgroundJobDialog(context.getCore(), context.getCluster(), e -> cancel());
        dialog.setHeaderTitle("Delete " + context.getSelected().size() + " " + (context.getSelected().size() > 1 ? "Items": "Item") );
        dialog.setMax(context.getSelected().size());
        dialog.open();

        if (parallelExecution.getValue()) {
            // start all in parallel
            final List<Thread> threads = new LinkedList<>();

            context.getSelected().forEach(res -> {
                if (canceled) return;
                final var thread = Thread.startVirtualThread(() -> {
                    try (var sce = context.getSecurityContext().enter()) {
                        deleteItem(res);
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
                    if (canceled) return;
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
                    if (canceled) return;
                    try (var sce = context.getSecurityContext().enter()) {
                        context.getUi().access(() -> {
                            dialog.setProgress(dialog.getProgress() + 1, res.getMetadata().getNamespace() + "." + res.getMetadata().getName());
                        });
                        deleteItem(res);
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

    private void cancel() {
        canceled = true;
    }

    private void deleteItem(KubernetesObject res) throws ApiException {
        var handler = k8s.getTypeHandler(res, context.getCluster(), context.getType());
        handler.delete(context.getCluster().getApiProvider(), res.getMetadata().getName(), res.getMetadata().getNamespace());
        if (sleepBetweenInMs.getValue() > 0) {
            LOGGER.debug("Wait after delete {} ms", sleepBetweenInMs.getValue());
            MThread.sleep(sleepBetweenInMs.getValue());
        }
        if (waitForDisappear.getValue()) {
            long timeout = waitDisappearTimeoutInSec.getValue() * 1000;
            long startTime = System.currentTimeMillis();
            while(!MPeriod.isTimeOut(startTime, timeout)) {
                if (canceled) return;
                LOGGER.debug("Wait for resource to disappear {}", K8s.displayName(res));
                try {
                    handler.get(context.getCluster().getApiProvider(), res.getMetadata().getName(), res.getMetadata().getNamespace());
                } catch (ApiException e) {
                    LOGGER.debug("Resource disappeared {}", K8s.displayName(res));
                    return;
                }
                MThread.sleep(1000);
            }
            LOGGER.debug("Disappear timeout reached for resource {}", K8s.displayName(res));
        }
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
