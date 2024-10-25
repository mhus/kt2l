package de.mhus.kt2l.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.Core;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.config.service.Get;

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
