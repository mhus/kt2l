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
package de.mhus.kt2l.resources.cronjob;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.aaa.UsersConfiguration;
import de.mhus.kt2l.aaa.WithRole;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.ResourceAction;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1CronJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@WithRole(UsersConfiguration.ROLE.WRITE)
public class CronJobSuspendAction implements ResourceAction {

    @Autowired
    private CronJobK8s handler;

    @Override
    public boolean canHandleType(Cluster cluster, V1APIResource type) {
        return K8s.CRON_JOB.equals(type);
    }

    @Override
    public boolean canHandleResource(Cluster cluster, V1APIResource type, Set<? extends KubernetesObject> selected) {
        return canHandleType(cluster, type) && selected.size() > 0;
    }

    @Override
    public void execute(ExecutionContext context) {
        var foundNotSuspended = new AtomicInteger(0);
        var foundSuspended = new AtomicInteger(0);
        context.getSelected().forEach(resource -> {
            if (resource instanceof V1CronJob cronJob) {
                if (cronJob.getSpec().getSuspend() == null || !cronJob.getSpec().getSuspend()) {
                    foundNotSuspended.incrementAndGet();
                } else {
                    foundSuspended.incrementAndGet();
                }
            }
        });

        if (foundNotSuspended.get() > 0) {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Suspend CronJob");
            dialog.setText("Do you really want to suspend " + foundNotSuspended.get() + " CronJob(s)?");
            dialog.setConfirmText("Suspend");
            dialog.setCancelText("Cancel");
            dialog.setCancelable(true);
            dialog.setConfirmButtonTheme("error primary");
            dialog.setCancelButtonTheme("tertiary");
            dialog.addConfirmListener(event -> {
                context.getSelected().forEach(resource -> {
                    if (resource instanceof V1CronJob cronJob) {
                        if (cronJob.getSpec().getSuspend() == null || !cronJob.getSpec().getSuspend()) {
                            cronJob.getSpec().setSuspend(true);
                            try {
                                handler.replaceResource(context.getCluster().getApiProvider(), cronJob.getMetadata().getName(), cronJob.getMetadata().getNamespace(), cronJob );
                                UiUtil.showSuccessNotification("CronJob " + cronJob.getMetadata().getName() + " suspended");
                            } catch (Exception e) {
                                UiUtil.showErrorNotification("Error suspending CronJob " + cronJob.getMetadata().getName(), e);
                            }
                        }
                    }
                });
            });
            dialog.open();
        } else {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Resume CronJob");
            dialog.setText("Do you really want to resume " + foundSuspended.get() + " CronJob(s)?");
            dialog.setConfirmText("Resume");
            dialog.setCancelText("Cancel");
            dialog.setCancelable(true);
            dialog.setConfirmButtonTheme("error primary");
            dialog.setCancelButtonTheme("tertiary");
            dialog.addConfirmListener(event -> {
                context.getSelected().forEach(resource -> {
                    if (resource instanceof V1CronJob cronJob) {
                        if (cronJob.getSpec().getSuspend() != null && cronJob.getSpec().getSuspend()) {
                            cronJob.getSpec().setSuspend(false);
                            try {
                                handler.replaceResource(context.getCluster().getApiProvider(), cronJob.getMetadata().getName(), cronJob.getMetadata().getNamespace(), cronJob );
                                UiUtil.showSuccessNotification("CronJob " + cronJob.getMetadata().getName() + " resumed");
                            } catch (Exception e) {
                                UiUtil.showErrorNotification("Error resuming CronJob " + cronJob.getMetadata().getName(), e);
                            }
                        }
                    }
                });
            });
            dialog.open();
        }

    }

    @Override
    public String getTitle() {
        return "Suspend/Resume";
    }

    @Override
    public String getMenuPath() {
        return ResourceAction.ACTIONS_PATH;
    }

    @Override
    public int getMenuOrder() {
        return ResourceAction.ACTIONS_ORDER + 211;
    }

    @Override
    public String getShortcutKey() {
        return "s";
    }

    @Override
    public String getDescription() {
        return "Toggle Suspend/Resume CronJob";
    }

    @Override
    public AbstractIcon getIcon() {
        return VaadinIcon.POWER_OFF.create();
    }
}
