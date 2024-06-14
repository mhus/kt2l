package de.mhus.kt2l.development;

import com.sun.management.UnixOperatingSystemMXBean;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.mhus.commons.tools.MSystem;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.generated.DeployInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Objects;

import static de.mhus.commons.tools.MLang.tryThis;

@Configurable
public class DevelopmentPanel extends VerticalLayout implements DeskTabListener {

    @Autowired
    private Configuration config;

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
        i.append("KT2L Development Panel\n");
        i.append("-----------------------\n");
        i.append("Counter: " + counter + "\n");
        i.append("Core Element Count   : " + deskTab.getTabBar().getCore().getContent().getChildren().count() + "\n");
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
        i.append("Browser App    : " + tryThis(() -> deskTab.getTabBar().getCore().ui().getSession().getBrowser().getBrowserApplication()).or("?") + "\n");
        i.append("Browser Locale : " + tryThis(() -> deskTab.getTabBar().getCore().ui().getSession().getBrowser().getLocale().toString()).or("?") + "\n");
        i.append("Browser Address: " + tryThis(() -> deskTab.getTabBar().getCore().ui().getSession().getBrowser().getAddress()).or("?") + "\n");

        info.setValue(i.toString());
    }
}
