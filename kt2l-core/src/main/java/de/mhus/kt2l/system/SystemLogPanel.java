package de.mhus.kt2l.system;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.commons.lang.IRegistration;
import de.mhus.commons.tools.MDate;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.core.Tail;
import de.mhus.kt2l.core.TailRow;
import de.mhus.kt2l.core.UiUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.Objects;
import java.util.function.Consumer;

@Slf4j
@Configurable
public class SystemLogPanel extends VerticalLayout implements DeskTabListener, Consumer<ILoggingEvent> {

    @Autowired
    private LogbackEventProvider logbackConfiguration;

    private Tail logs;
    private IRegistration registration;
    private DeskTab deskTab;
    private Core core;

    @Override
    public void tabInit(DeskTab deskTab) {
        this.deskTab = deskTab;
        this.core = deskTab.getTabBar().getCore();
        logs = new Tail();
        logs.setMaxRows(1000);
        logs.addClassName("log-view");
        logs.setSizeFull();
        add(logs);

        setSizeFull();
        setPadding(false);
        setMargin(false);

        registration = logbackConfiguration.getEventHandler().registerWeak(this);
        LOGGER.info("Logging Panel Started");
    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabUnselected() {

    }

    @Override
    public void tabDestroyed() {
        if (registration != null)
            registration.unregister();
    }

    @Override
    public void tabRefresh(long counter) {

    }

    @Override
    public void accept(ILoggingEvent event) {
        core.ui().access(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append(MDate.toIsoDateTime(event.getTimeStamp())).append(" ");
            sb.append(event.getLevel()).append(" ");
            MString.fillUntil(sb, 30, ' ');
            sb.append(event.getFormattedMessage()).append(" (");
            sb.append(event.getLoggerName()).append(")");

            var row = TailRow.builder().text(sb.toString());
            row.color(switch (event.getLevel().toInt()) {
                case Level.ERROR_INT: yield UiUtil.COLOR.RED;
                case Level.WARN_INT: yield UiUtil.COLOR.YELLOW;
                case Level.INFO_INT: yield UiUtil.COLOR.GREEN;
                case Level.DEBUG_INT: yield UiUtil.COLOR.GREY;
                default: yield UiUtil.COLOR.NONE;
            });
            var r = row.build();
            logs.addRow(r);

            if (event.getThrowableProxy() != null) {
                var t = event.getThrowableProxy();
                logs.addRow(TailRow.builder().text(t.getMessage()).color(r.getColor()).build());
                for (var line : t.getStackTraceElementProxyArray())
                    logs.addRow(TailRow.builder().text("  " + line.toString()).color(r.getColor()).build());
            }


        });
    }
}
