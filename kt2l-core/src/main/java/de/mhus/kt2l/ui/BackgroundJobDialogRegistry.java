package de.mhus.kt2l.ui;

import com.vaadin.flow.component.dialog.Dialog;
import de.mhus.commons.tools.MSystem;
import de.mhus.commons.util.WeakList;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.Core;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

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
