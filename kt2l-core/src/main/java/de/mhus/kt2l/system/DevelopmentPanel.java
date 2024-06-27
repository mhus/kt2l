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

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.mhus.commons.console.ConsoleTable;
import de.mhus.commons.tools.MCast;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.resources.ResourcesGridPanel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Objects;

import static de.mhus.commons.tools.MLang.tryThis;
import static org.apache.logging.log4j.util.Strings.isBlank;

@Configurable
@Slf4j
public class DevelopmentPanel extends VerticalLayout implements DeskTabListener {

    private final boolean evilMode;
    @Autowired
    private Configuration config;
    @Autowired
    private UpTimeService upTimeService;
    @Autowired
    private PanelService panelService;

    private TextArea info;
    private DeskTab deskTab;
    private OperatingSystemMXBean osBean;
    private TextArea output;
    private String browserMemoryUsage;

    public DevelopmentPanel(boolean evilMode) {
        this.evilMode = evilMode;
    }

    @Override
    public void tabInit(DeskTab deskTab) {
        this.deskTab = deskTab;
        info = new TextArea();
        info.addClassName("development-info");
        info.setReadOnly(true);
        info.setWidth("100%");
        add(info);

        var bar = new MenuBar();
        bar.addItem("Env", e -> showEnv());
        bar.addItem("Props", e -> showProps());
        bar.addItem("TabBarInfo", e -> showTabBarInfo());
        bar.addItem("Core Panels", e -> showCorePanels());
        bar.addItem("Grid History", e -> showGridHistory());
        if(evilMode) {
            bar.addItem("Logs", e -> panelService.showSystemLogPanel(deskTab.getTabBar().getCore()).select());
        }
        var ebcmItem = bar.addItem("EBCM", e -> deskTab.getTabBar().getCore().getGeneralContextMenu().setTarget(null));
        ebcmItem.getElement().setAttribute("title", "Enable Browser Context Menu");
        ebcmItem.setEnabled(deskTab.getTabBar().getCore().getGeneralContextMenu() != null);
//        bar.addItem("Exception", e -> LOGGER.warn("Test", new RuntimeException("Test Exception")));

        add(bar);

        output = new TextArea();
        output.addClassName("development-info");
        output.setReadOnly(true);
        output.setWidth("100%");
        add(output);


        var about = new Anchor("https://mhus.de", "The project KT2L was created by Mike Hummel in 2024. Aloha!");
        about.setTarget("_blank");
        add(about);

        osBean = ManagementFactory.getOperatingSystemMXBean();
        updateInfo(0);
    }

    private void showGridHistory() {
        StringBuilder i = new StringBuilder();
        i.append("Grid History\n");
        i.append("-----------------------\n");
        deskTab.getTabBar().getTabs().forEach(tab -> {
            if (tab.getPanel() instanceof ResourcesGridPanel gridPanel) {
                i.append("Grid: " + tab.getTabId() + " " + tab.getWindowTitle() + "\n");
                i.append("  Pointer: ").append(gridPanel.getHistoryPointer()).append("\n");
                ConsoleTable table = new ConsoleTable();
                table.setHeaderValues("Namespace", "ResourceType", "Filter Text", "Filter", "Sort Order", "Sort Ascending");
                gridPanel.getHistroy().forEach(h -> {
                    table.addRowValues(h.namespace(), K8s.displayName(h.type()), h.filterText(), h.filter() != null ? h.filter().getDescription() : "", h.sortOrder(), h.sortAscending());
                });
                i.append(table).append("\n");
            }
        });
        output.setValue(i.toString());
    }

    private void showProps() {
        StringBuffer i = new StringBuffer();
        i.append("Properties\n");
        i.append("-----------------------\n");
        System.getProperties().forEach((k, v) -> {
            i.append(k + "=" + v + "\n");
        });
        output.setValue(i.toString());
    }

    private void showEnv() {
        StringBuffer i = new StringBuffer();
        i.append("Environment\n");
        i.append("-----------------------\n");
        System.getenv().forEach((k, v) -> {
            i.append(k + "=" + v + "\n");
        });
        output.setValue(i.toString());
    }

    private void showCorePanels() {
        StringBuffer i = new StringBuffer();
        i.append("KT2L Core Panels\n");
        i.append("-----------------------\n");
        deskTab.getTabBar().getCore().getContent().getChildren().forEach(c -> {
            i.append("Panel: " + c.getClass().getCanonicalName() + "\n");
        });
        output.setValue(i.toString());
    }

    private void showTabBarInfo() {
        StringBuffer i = new StringBuffer();
        i.append("KT2L TabBar Info\n");
        i.append("-----------------------\n");
        i.append("Core Element Count   : " + deskTab.getTabBar().getCore().getContent().getChildren().count() + "\n");
        for (var tab : deskTab.getTabBar().getTabs()) {
            i.append("Tab           : " + tab.getTabId() + "\n");
            i.append("  Tab Class   : " + tryThis(() -> tab.getPanel().getClass().getCanonicalName()) + "\n");
            i.append("  Window Title: " + tab.getWindowTitle() + "\n");
            i.append("  Help Context: " + tab.getHelpContext() + "\n");
            i.append("  Flags       : P:" + tab.isReproducable() + " C:" + tab.isCloseable() + " V:" + tab.isVisible() + "\n");
        }

        output.setValue(i.toString());
    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabUnselected() {

    }

    @Override
    public void tabDestroyed() {

    }

    @Override
    public void tabRefresh(long counter) {

        if (counter % 3 == 0) {
            deskTab.getTabBar().getCore().ui().access(() -> {
                updateInfo(counter);
            });
        }

    }

    private void updateInfo(long counter) {

        getElement().executeJs("return performance && performance.memory ? performance.memory.jsHeapSizeLimit + \" \" + performance.memory.totalJSHeapSize + \" \" + performance.memory.usedJSHeapSize : \"\"").then(String.class, value -> {
            this.browserMemoryUsage = value;
        });

        StringBuffer i = new StringBuffer();
        i.append("KT2L Development Panel\n");
        i.append("-----------------------\n");
        SystemInfoPanel.fillInfo(counter, osBean, deskTab, upTimeService, i);

        i.append("Counter: " + counter + "\n");
        i.append("Core Panels Count    : " + deskTab.getTabBar().getCore().getContent().getChildren().count() + "\n");
        i.append("Core Background Count: " + deskTab.getTabBar().getCore().getBackgroundJobCount() + "\n");
        i.append("UI                   : " + Objects.hashCode(deskTab.getTabBar().getCore().ui()) + "\n");
        i.append("Session Id           : " + tryThis(() -> deskTab.getTabBar().getCore().ui().getSession().getSession().getId() ).orElse("?") + "\n");
        i.append("CumulativeRequestDuration: " + tryThis(() -> String.valueOf(deskTab.getTabBar().getCore().ui().getSession().getCumulativeRequestDuration()) ).orElse("?") + "\n");

        if (!isBlank(browserMemoryUsage)) {
            var parts = browserMemoryUsage.split(" ");
            var jsLimit = MCast.tolong(parts[0], 0);
            var jsTotal = MCast.tolong(parts[1], 0);
            var jsUsed = MCast.tolong(parts[2], 0);
            i.append("Browser Memory (3sec): " + MString.toByteDisplayString(jsUsed) + " / " + MString.toByteDisplayString(jsLimit) + " / " + MString.toByteDisplayString(jsTotal) + "\n");
        }

        info.setValue(i.toString());
    }
}
