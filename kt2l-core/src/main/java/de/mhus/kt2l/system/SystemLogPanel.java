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
package de.mhus.kt2l.system;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.commons.lang.IRegistration;
import de.mhus.commons.tools.MDate;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.ui.Tail;
import de.mhus.kt2l.ui.TailRow;
import de.mhus.kt2l.ui.UiUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

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
    private Level level = Level.DEBUG;
    private MenuItem debugItem;
    private MenuItem infoItem;
    private MenuItem warnItem;
    private MenuItem errorItem;

    @Override
    public void tabInit(DeskTab deskTab) {
        this.deskTab = deskTab;
        this.core = deskTab.getTabBar().getCore();

        var menuBar = new MenuBar();
        menuBar.addItem("Clear", e -> logs.clear());
        var levelItem = menuBar.addItem("Level");
        var levelMenu = levelItem.getSubMenu();
        debugItem = levelMenu.addItem("Debug", e -> {
            level = Level.DEBUG;
            updateLevelMenu();
        });
        debugItem.setCheckable(true);
        infoItem = levelMenu.addItem("Info", e -> {
            level = Level.INFO;
            updateLevelMenu();
        });
        infoItem.setCheckable(true);
        warnItem = levelMenu.addItem("Warn", e -> {
            level = Level.WARN;
            updateLevelMenu();
        });
        warnItem.setCheckable(true);
        errorItem = levelMenu.addItem("Error", e -> {
            level = Level.ERROR;
            updateLevelMenu();
        });
        errorItem.setCheckable(true);
        updateLevelMenu();

        add(menuBar);

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

    private void updateLevelMenu() {
        debugItem.setChecked(level == Level.DEBUG);
        infoItem.setChecked(level == Level.INFO);
        warnItem.setChecked(level == Level.WARN);
        errorItem.setChecked(level == Level.ERROR);
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

        if (event.getLevel().toInt() < level.toInt()) return;

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
