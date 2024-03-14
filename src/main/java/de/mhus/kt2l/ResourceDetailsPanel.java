package de.mhus.kt2l;

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import de.mhus.commons.yaml.MYaml;
import de.mhus.commons.yaml.YElement;
import de.mhus.commons.yaml.YMap;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Yaml;

import java.util.Map;

public class ResourceDetailsPanel extends VerticalLayout implements XTabListener {

    private final Object resource;
    private TextArea yamlArea;
    private String yamlText;
    private String managedFields;
    private Tabs tabs;
    private TextArea yamlFields;

    public ResourceDetailsPanel(ClusterConfiguration.Cluster clusterConfiguration, CoreV1Api api, MainView mainView, Object resource) {
        this.resource = resource;
    }


    @Override
    public void tabInit(XTab xTab) {

        yamlText = Yaml.dump(resource);
        YElement yDocument = MYaml.loadFromString(yamlText);
        YMap yMetadata = yDocument.asMap().getMap("metadata");
        YMap yManagedFields = null;
        if (yMetadata != null) {
            yManagedFields = yMetadata.getMap("managedFields");
                    ((Map<String, Object>) yMetadata.getObject()).remove("managedFields");
        }
        yamlText = MYaml.toString(yDocument);
        if (yManagedFields != null) {
            managedFields = MYaml.toString(yManagedFields);
        }
        

        yamlArea = new TextArea();
        yamlArea.setValue(yamlText);
        yamlArea.setSizeFull();
        yamlArea.setReadOnly(true);
        
        yamlFields = new TextArea();
        yamlFields.setValue(managedFields);
        yamlFields.setSizeFull();
        yamlFields.setReadOnly(true);

        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Yaml", yamlArea);
        tabSheet.add("Managed Fields", yamlFields);

        tabSheet.setSizeFull();
        add(tabSheet);

    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabDeselected() {

    }

    @Override
    public void tabDestroyed() {

    }

    @Override
    public void tabRefresh() {

    }

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }
}
