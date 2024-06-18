package de.mhus.kt2l.development;

import com.sun.management.UnixOperatingSystemMXBean;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.mhus.commons.tools.MCast;
import de.mhus.commons.tools.MDate;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tools.MSystem;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
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
