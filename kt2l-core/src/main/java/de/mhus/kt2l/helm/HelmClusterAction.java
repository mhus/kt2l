package de.mhus.kt2l.helm;

import com.marcnuri.helm.Helm;
import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.server.StreamResource;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.cluster.ClusterAction;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.k8s.K8sService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

//@Component
@Slf4j
public class HelmClusterAction implements ClusterAction {

    @Autowired
    private K8sService k8sService;

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandle(Core core) {
        try {
            var version = Helm.version().call();
            LOGGER.info("Helm version: {}", version);
            return version != null;
        } catch (Exception e) {
            LOGGER.warn("Helm not available", e);
            return false;
        }
    }

    @Override
    public boolean canHandle(Core core, Cluster cluster) {
        return k8sService.getKubeConfigPath(cluster) != null;
    }

    @Override
    public String getTitle() {
        return "Helm";
    }

    @Override
    public void execute(Core core, Cluster cluster) {
        panelService.addHelmChartPanel(core, cluster).select();
    }

    @Override
    public AbstractIcon getIcon() {
        return getHelmIcon();
    }

    @Override
    public int getPriority() {
        return 3405;
    }


    public static SvgIcon getHelmIcon() {
        StreamResource iconResource = new StreamResource("helm-logo.svg",
                () -> HelmClusterAction.class.getResourceAsStream("/images/helm-logo.svg"));
        SvgIcon icon = new SvgIcon(iconResource);
        return icon;
    }
}
