package de.mhus.kt2l.events;

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.commons.lang.IRegistration;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterBackgroundJob;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.core.Tail;
import de.mhus.kt2l.core.TailRow;
import de.mhus.kt2l.core.UiUtil;
import de.mhus.kt2l.resources.ExecutionContext;
import de.mhus.kt2l.resources.util.ResourceManager;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.util.Watch;

import java.util.ArrayList;

public class EventPanel extends VerticalLayout implements DeskTabListener {

    private final Core core;
    private final Cluster cluster;
    private final ResourceManager<KubernetesObject> resourceManager;
    private IRegistration eventRegistration;
    private Tail eventList;
    private MenuItem menuItemAutoScroll;
    private MenuItem menuItemWrapLines;

    public EventPanel(ExecutionContext context) {
        this.core = context.getCore();
        this.cluster = context.getCluster();
        this.resourceManager = new ResourceManager<KubernetesObject>(new ArrayList<>(context.getSelected()), true);
    }

    public EventPanel(Core core, Cluster cluster) {
        this.core = core;
        this.cluster = cluster;
        this.resourceManager = null;
    }

    @Override
    public void tabInit(DeskTab deskTab) {

        var menuBar = new MenuBar();

        if (resourceManager != null) {
            resourceManager.injectMenu(menuBar);
        }

        var viewMenuItem = menuBar.addItem("View");
        var viewMenu = viewMenuItem.getSubMenu();
        menuItemAutoScroll = viewMenu.addItem("Autoscroll", e -> {
            eventList.setAutoScroll(menuItemAutoScroll.isChecked());
        });
        menuItemAutoScroll.setCheckable(true);
        menuItemAutoScroll.setChecked(true);

        menuItemWrapLines = viewMenu.addItem("Wrap lines", e -> {
            if (menuItemWrapLines.isChecked())
                eventList.addClassNames("events-view-nowrap");
            else
                eventList.removeClassNames("events-view-nowrap");
        });
        menuItemWrapLines.setCheckable(true);

        add(menuBar);

        eventList = new Tail();
        eventList.addClassName("events-view");
        eventList.setSizeFull();
        eventList.setAutoScroll(true);
        eventList.setMaxRows(1000);

        add(eventList);

        this.eventRegistration = ClusterBackgroundJob.instance(
                core,
                cluster,
                EventWatch.class
        ).getEventHandler().registerWeak(this::changeEvent);

        setSizeFull();
    }

    private void changeEvent(Watch.Response<CoreV1Event> coreV1EventResponse) {
        if (resourceManager != null) {
            if (resourceManager.getResources().stream().filter(res -> res.getMetadata().getUid().equals(coreV1EventResponse.object.getInvolvedObject().getUid())).findFirst().isEmpty())
                return;
        }

        core.ui().access(() -> {
            eventList.addRow(TailRow.builder().text(createText(coreV1EventResponse.object)).color(createColor(coreV1EventResponse.object)).build());
        });
    }

    private UiUtil.COLOR createColor(CoreV1Event object) {
        if (object.getType().equals("Warning"))
            return UiUtil.COLOR.RED;
        return null;
    }

    private String createText(CoreV1Event event) {
        var sb = new StringBuilder();
        sb.append(event.getLastTimestamp()).append(" ");
        MString.fillUntil(sb, 21, ' ');
        sb.append(event.getType()).append(" ");
        MString.fillUntil(sb, 30, ' ');
        sb.append(event.getReason()).append(" ");
        MString.fillUntil(sb, 50, ' ');
        sb.append(event.getInvolvedObject().getName()).append(" ");
        MString.fillUntil(sb, 70, ' ');
        sb.append(event.getMessage());
        return sb.toString();
    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabUnselected() {

    }

    @Override
    public void tabDestroyed() {
        eventRegistration.unregister();
    }

    @Override
    public void tabRefresh(long counter) {

    }

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }
}
