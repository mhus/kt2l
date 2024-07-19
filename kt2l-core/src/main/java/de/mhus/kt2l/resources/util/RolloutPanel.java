package de.mhus.kt2l.resources.util;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import de.mhus.commons.lang.IRegistration;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.k8s.K8sRolloutHistory;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.util.Watch;
import io.kubernetes.client.util.labels.LabelSelector;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class RolloutPanel<T extends KubernetesObject> extends VerticalLayout {

    protected final Core core;
    protected final Cluster cluster;
    private final ProgressBar progress;
    private final Div text;
    private final VerticalLayout replicaSets;
    private final HorizontalLayout progressPanel;
    private final Button pauseBtn;
    private final Button startBtn;
    protected T target;
    private IRegistration registration;
    protected int targetReady = -1;
    protected int targetDesired = -1;
    protected int targetUnavailable = -1;
    protected boolean targetCanPause = false;
    protected boolean targetStarted = true;
    protected LabelSelector ownerLabelSelector = null;

    public RolloutPanel(Core core, Cluster cluster) {
        this.core = core;
        this.cluster = cluster;
        text = new Div("");
        text.setWidthFull();
        text.setHeight("36px");
        add(text);

        progressPanel = new HorizontalLayout();
        progressPanel.setVisible(false);
        progressPanel.setHeight("5px");
        progressPanel.setWidthFull();

        add(progressPanel);

        progress = new ProgressBar();
        progress.setSizeFull();
        progress.setMin(0);
        progress.setMax(1);
        progress.setValue(0);
        progressPanel.add(progress);

        pauseBtn = new Button(VaadinIcon.PAUSE.create());
        pauseBtn.setWidth("36px");
        pauseBtn.setHeight("36px");
        pauseBtn.addClickListener(this::actionPause);
        progressPanel.add(pauseBtn);

        startBtn = new Button(VaadinIcon.PLAY.create());
        startBtn.setWidth("36px");
        startBtn.setHeight("36px");
        startBtn.addClickListener(this::actionStart);
        progressPanel.add(startBtn);

        var scroller = new Scroller();
        scroller.setSizeFull();
        add(scroller);
        replicaSets = new VerticalLayout();
        replicaSets.setWidthFull();
        scroller.setContent(replicaSets);
    }

    private void actionStart(ClickEvent<Button> buttonClickEvent) {
        if (!targetCanPause) return;
        updateRunnng(true);
    }

    private void actionPause(ClickEvent<Button> buttonClickEvent) {
        if (!targetCanPause) return;
        updateRunnng(false);
    }

    protected abstract void updateRunnng(boolean running);

    public void close() {
        if (registration != null) registration.unregister();
        registration = null;
    }

    public void setTarget(T target) {
        setTarget(target, true);
    }

    protected void setTarget(T target, boolean updateReplicaSets) {
        this.target = target;
        watchEvents();
        if (target != null) {
            updateTarget();
            core.ui().access(() -> {
                updateView(updateReplicaSets);
            });
        } else {
            cleanTarget();
        }
    }

    private void updateView(boolean updateReplicaSets) {
        if (targetReady >= 0 && targetDesired >= 0 && targetReady < targetDesired) {
            progress.setIndeterminate(false);
            progressPanel.setVisible(true);
            progress.setMax(targetDesired);
            progress.setValue(targetReady);
            text.setText(targetReady + "/" + targetDesired + (targetUnavailable > 0 ? " (" + targetUnavailable + " unavailable)" : ""));
        } else if (targetReady >= 0 && targetDesired >= 0 && targetReady > targetDesired) {
            progress.setIndeterminate(false);
            progressPanel.setVisible(true);
            progress.setMax(targetReady);
            progress.setValue(targetDesired);
            text.setText(targetReady + "/" + targetDesired + (targetUnavailable > 0 ? " (" + targetUnavailable + " unavailable)" : ""));
        } else if (targetUnavailable > 0) {
            progressPanel.setVisible(true);
            progress.setIndeterminate(true);
            text.setText(targetUnavailable + " unavailable");
        } else if (targetReady < 0 && targetDesired < 0) {
            text.setText("");
            progressPanel.setVisible(false);
            replicaSets.removeAll();
        } else {
            progressPanel.setVisible(false);
            progress.setMax(1);
            progress.setValue(0);
            text.setText("No rollout in progress" + (target != null ? " for " + target.getMetadata().getName() : ""));
        }
        if (targetCanPause) {
            pauseBtn.setVisible(targetStarted);
            startBtn.setVisible(!targetStarted);
        } else {
            pauseBtn.setVisible(false);
            startBtn.setVisible(false);
        }
        if (updateReplicaSets) {
            replicaSets.removeAll();
            if (ownerLabelSelector != null) {
                try {
                    var rollout = new K8sRolloutHistory();
                    rollout.setApiClient(cluster.getApiProvider().getApiClient());
                    rollout.setApiTypeClass(target.getClass());
                    rollout.setName(target.getMetadata().getName());
                    rollout.setNamespace(target.getMetadata().getNamespace());
                    rollout.execute().forEach(h -> {
                        var entry = new Div(
                                "REV " + h.revision() +
                                " - " + h.changeCause()
                        );
                        entry.setWidthFull();
                        entry.setHeight("20px");
                        replicaSets.add(entry);
                    });
                } catch (Exception e) {
                    LOGGER.debug("Can't fetch ReplicaSets for {} {}", target.getMetadata().getName(), ownerLabelSelector, e);
                }
            }
        }
    }

    protected abstract void updateTarget();

    public void cleanTarget() {
        cleanTarget(true);
    }

    protected void cleanTarget(boolean updateReplicaSets) {
        targetDesired = -1;
        targetReady = -1;
        targetUnavailable = -1;
        ownerLabelSelector = null;
        target = null;
        core.ui().access(() -> {
            updateView(updateReplicaSets);
        });
    }

    protected synchronized void watchEvents() {
        if (registration == null)
            registration = core.backgroundJobInstance(cluster, getManagedWatchClass()).getEventHandler().registerWeak(this::changeEvent);
    }

    protected abstract Class<? extends ClusterBackgroundJob> getManagedWatchClass();

    private void changeEvent(Watch.Response<KubernetesObject> event) {
        if (target == null || event.object == null) return;
        if (event.object.getMetadata().getName().equals(target.getMetadata().getName()) && event.object.getMetadata().getNamespace().equals(target.getMetadata().getNamespace()) ) {
            switch (event.type) {
                case "ADDED", "MODIFIED" -> {
                    setTarget((T)event.object, false);
                }
                case "DELETED" -> {
                    cleanTarget(false);
                }
            }
        }
    }

}
