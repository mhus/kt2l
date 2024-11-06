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

import com.vaadin.flow.component.dialog.Dialog;
import de.mhus.commons.tools.MSystem;
import de.mhus.commons.util.WeakList;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.Core;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class BackgroundJobDialogRegistry extends ClusterBackgroundJob {

    private final WeakList<BackgroundJobDialog> dialogs = new WeakList<>();

    @Override
    public void close() {
        getDialogs().forEach(BackgroundJobDialog::close);
    }

    @Override
    public void init(Core core, String clusterId, String jobId) throws IOException {

    }

    public void register(BackgroundJobDialog dialog) {
        LOGGER.debug("Register dialog {}", MSystem.getObjectId(dialog));
        dialogs.add(dialog);
    }

    public void unregister(BackgroundJobDialog dialog) {
        LOGGER.debug("Unregister dialog {}", MSystem.getObjectId(dialog));
        dialogs.remove(dialog);
    }

    public List<BackgroundJobDialog> getDialogs() {
        return dialogs.stream().sorted(Comparator.comparing(Dialog::getHeaderTitle)).toList();
    }

}
