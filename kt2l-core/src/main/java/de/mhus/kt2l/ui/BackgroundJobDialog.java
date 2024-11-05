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
package de.mhus.kt2l.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.Core;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public class BackgroundJobDialog extends ProgressDialog {

    private final Core core;
    private final Cluster cluster;
    @Getter
    private final boolean cancelable;
    private final Consumer<BackgroundJobDialog> cancelAction;
    private final BackgroundJobDialogRegistry registry;
    @Getter
    private boolean canceled = false;

    public BackgroundJobDialog(Core core, Cluster cluster, boolean cancelable) {
        this(core, cluster, d -> {});
    }

    public BackgroundJobDialog(Core core, Cluster cluster, Consumer<BackgroundJobDialog> cancelAction) {
        this.core = core;
        this.cluster = cluster;
        this.cancelable = cancelAction != null;
        this.cancelAction = cancelAction;

        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);

        if (cancelable) {
            Button cancel = new Button("Cancel");
            cancel.addThemeVariants(ButtonVariant.LUMO_ERROR);
            cancel.addClickListener(e -> {
                cancel();
            });
            add(cancel);
        }

        registry = core.backgroundJobInstance(cluster, BackgroundJobDialogRegistry.class);
        registry.register(this);
    }

    public synchronized void cancel() {
        if (canceled) return;
        canceled = true;
        if (cancelAction != null) {
            cancelAction.accept(this);
        }
    }

    public void close() {
        registry.unregister(this);
        super.close();
    }


}
