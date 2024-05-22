package de.mhus.kt2l.cluster;

import com.vaadin.flow.component.Component;
import de.mhus.kt2l.cfg.panel.CPanelVerticalLayout;
import de.mhus.kt2l.cfg.panel.YArray;
import de.mhus.kt2l.cfg.panel.YBoolean;
import de.mhus.kt2l.cfg.panel.YCombobox;
import de.mhus.kt2l.cfg.panel.YComponent;
import de.mhus.kt2l.cfg.panel.YText;
import de.mhus.kt2l.core.UiUtil;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@Configurable
public class ClusterCfgPanel extends CPanelVerticalLayout {

    private List<YComponent> components = new LinkedList<>();

    @Autowired
    private K8sService k8s;

    @Override
    public Component getPanel() {
        return this;
    }

    @Override
    public String getTitle() {
        return "Clusters";
    }

    @Override
    public void initUi() {
        add(new YText()
                .name("defaultCluster")
                .label("Default Cluster")
                .defaultValue(""));
        add(new YText()
                .name("defaultNamespace")
                .label("Default Namespace")
                .defaultValue(""));
        add(new YCombobox()
                .values(Arrays.stream(K8s.values()).map(v -> v.kind()).toList())
                .name("defaultResourceType")
                .label("Default Resource Type").defaultValue("pod"));
        add(new YArray()
                .create(p -> {
                            p.add(new YCombobox()
                                    .values(k8s.getAvailableContexts())
                                    .name("name")
                                    .label("Name")
                                    .defaultValue(""));
                            p.add(new YText()
                                    .name("title")
                                    .label("Title")
                                    .defaultValue(""));
                            p.add(new YBoolean()
                                    .name("enabled")
                                    .label("Enabled")
                                    .defaultValue(true));
                            p.add(new YCombobox()
                                    .values(Arrays.stream(UiUtil.COLOR.values()).map(v -> v.name()).toList())
                                    .name("color")
                                    .label("Color").defaultValue("NONE"));
                            p.add(new YText()
                                    .name("apiProviderTimeout")
                                    .label("Api Provider Timeout")
                                    .defaultValue("5m"));
                        }
                )
                .name("clusters")
                .label("Clusters"));
    }

}
