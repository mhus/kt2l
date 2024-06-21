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

import com.sun.management.UnixOperatingSystemMXBean;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.mhus.commons.tools.MCast;
import de.mhus.commons.tools.MDate;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tools.MSystem;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.generated.DeployInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import static de.mhus.commons.tools.MLang.tryThis;
import static org.apache.logging.log4j.util.Strings.isBlank;

@Configurable
public class SystemInfoPanel extends VerticalLayout implements DeskTabListener {

    @Autowired
    private Configuration config;
    @Autowired
    private UpTimeService upTimeService;
    @Autowired
    private PanelService panelService;
    private TextArea info;
    private DeskTab deskTab;
    private OperatingSystemMXBean osBean;

    @Override
    public void tabInit(DeskTab deskTab) {
        this.deskTab = deskTab;
        info = new TextArea();
        info.addClassName("development-info");
        info.setReadOnly(true);
        info.setWidth("100%");
        add(info);

        var menuBar = new MenuBar();
        menuBar.addItem("KT2L Logs", e -> panelService.showSystemLogPanel(deskTab.getTabBar().getCore()).select());
        add(menuBar);

        osBean = ManagementFactory.getOperatingSystemMXBean();
        updateInfo(0);
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
        StringBuffer i = new StringBuffer();
        fillInfo(counter, osBean, deskTab, upTimeService, i);
        info.setValue(i.toString());

    }

    static void fillInfo(long counter, OperatingSystemMXBean osBean, DeskTab deskTab,  UpTimeService upTimeService, StringBuffer i) {
        i.append("DeployInfo     : " + DeployInfo.VERSION + " " + DeployInfo.CREATED + "\n");
        i.append("Java VM Version: " + System.getProperty("java.version") + "/" + System.getProperty("java.vm.name") + "/" + System.getProperty("java.vendor") + "\n");
        i.append("Architecture   : " + System.getProperty("os.arch") + "/" + System.getProperty("os.name") + "/" + System.getProperty("os.version") + "\n");
        i.append("Active Sessions: " + CoreCounterListener.getCounter() + "\n");
        i.append("Local Time     : " + MDate.toIso8601(System.currentTimeMillis()) + "\n");
        i.append("Up Time        : " + upTimeService.getUpTimeFormatted() + "\n");
        i.append("\n");
        i.append("Memory         : " + MSystem.freeMemoryAsString() + " / " + MSystem.maxMemoryAsString() + "\n");
        i.append("Threads        : " + Thread.getAllStackTraces().size() + "\n");
        if (osBean != null) {
        i.append("OS             : " + osBean.getName() + " " + osBean.getVersion() + " " + osBean.getArch() + "\n");
        i.append("OS Load        : " + osBean.getSystemLoadAverage() + "\n");
        i.append("OS CPUs        : " + osBean.getAvailableProcessors() + "\n");
        if (osBean instanceof UnixOperatingSystemMXBean unixOsBean) {
        i.append("OS Open File   : " + unixOsBean.getOpenFileDescriptorCount() + " / " + unixOsBean.getMaxFileDescriptorCount() + "\n");
        }
        }
        i.append("\n");
        i.append("Browser App    : " + tryThis(() -> deskTab.getTabBar().getCore().ui().getSession().getBrowser().getBrowserApplication()).or("?") + "\n");
        i.append("Browser Locale : " + tryThis(() -> deskTab.getTabBar().getCore().ui().getSession().getBrowser().getLocale().toString()).or("?") + "\n");
        i.append("Browser Address: " + tryThis(() -> deskTab.getTabBar().getCore().ui().getSession().getBrowser().getAddress()).or("?") + "\n");
        var browserMemoryUsage = deskTab.getTabBar().getCore().getBrowserMemoryUsage();
        if (!isBlank(browserMemoryUsage)) {
        var parts = browserMemoryUsage.split(" ");
        var jsLimit = MCast.tolong(parts[0], 0);
        var jsTotal = MCast.tolong(parts[1], 0);
        var jsUsed = MCast.tolong(parts[2], 0);
        i.append("Browser Memory : " + MString.toByteDisplayString(jsUsed) + " / " + MString.toByteDisplayString(jsLimit) + " / " + MString.toByteDisplayString(jsTotal) + "\n");
        }
    }
}
