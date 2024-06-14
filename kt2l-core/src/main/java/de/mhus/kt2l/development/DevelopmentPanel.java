package de.mhus.kt2l.development;

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
import de.mhus.kt2l.generated.DeployInfo;
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

    @Autowired
    private Configuration config;

    private TextArea info;
    private DeskTab deskTab;
    private OperatingSystemMXBean osBean;
    private TextArea output;
    private String browserMemoryUsage;

    @Override
    public void tabInit(DeskTab deskTab) {
        this.deskTab = deskTab;
        info = new TextArea();
        info.addClassName("development-info");
        info.setReadOnly(true);
        info.setWidth("100%");
        add(info);

        var bar = new MenuBar();
        bar.addItem("TabBarInfo", e -> showTabBarInfo());
        bar.addItem("Core Panels", e -> showCorePanels());
        add(bar);

        output = new TextArea();
        output.addClassName("development-info");
        output.setReadOnly(true);
        output.setWidth("100%");
        add(output);



        osBean = ManagementFactory.getOperatingSystemMXBean();
        updateInfo(0);
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
        i.append("Counter: " + counter + "\n");
        i.append("Local Time: " + MDate.toIso8601(System.currentTimeMillis()) + "\n");
        i.append("Core Panels Count    : " + deskTab.getTabBar().getCore().getContent().getChildren().count() + "\n");
        i.append("Core Background Count: " + deskTab.getTabBar().getCore().getBackgroundJobCount() + "\n");
        i.append("DeployInfo: " + DeployInfo.VERSION + " " + DeployInfo.CREATED + "\n");
        i.append("UI        : " + Objects.toIdentityString(deskTab.getTabBar().getCore().ui()) + "\n");
        i.append("Session   : " + tryThis(() -> Objects.toIdentityString(deskTab.getTabBar().getCore().ui().getSession())).or("?") + "\n");
        i.append("CumulativeRequestDuration: " + tryThis(() -> String.valueOf(deskTab.getTabBar().getCore().ui().getSession().getCumulativeRequestDuration()) ).or("?") + "\n");

        i.append("Core instances: " + CoreCounterListener.getCounter() + "\n");

        i.append("Memory : " + MSystem.freeMemoryAsString() + " / " + MSystem.maxMemoryAsString() + "\n");
        i.append("Threads: " + Thread.getAllStackTraces().size() + "\n");
        if (osBean != null) {
            i.append("OS     : " + osBean.getName() + " " + osBean.getVersion() + " " + osBean.getArch() + "\n");
            i.append("OS Load: " + osBean.getSystemLoadAverage() + "\n");
            i.append("CPUs   : " + osBean.getAvailableProcessors() + "\n");
            if (osBean instanceof UnixOperatingSystemMXBean unixOsBean) {
                i.append("OS Open File Descriptor Count: " + unixOsBean.getOpenFileDescriptorCount() + " / " + unixOsBean.getMaxFileDescriptorCount() + "\n");
            }
        }

        if (!isBlank(browserMemoryUsage)) {
            var parts = browserMemoryUsage.split(" ");
            var jsLimit = MCast.tolong(parts[0], 0);
            var jsTotal = MCast.tolong(parts[1], 0);
            var jsUsed = MCast.tolong(parts[2], 0);
            i.append("Browser Memory: " + MString.toByteDisplayString(jsUsed) + " / " + MString.toByteDisplayString(jsLimit) + " / " + MString.toByteDisplayString(jsTotal) + "\n");
        }

        info.setValue(i.toString());
    }
}
