package de.mhus.kt2l;

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.Tabs;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import de.mhus.commons.yaml.MYaml;
import de.mhus.commons.yaml.YElement;
import de.mhus.commons.yaml.YMap;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Yaml;

import java.util.Map;

public class ResourceDetailsPanel extends VerticalLayout implements XTabListener {

    private final Object resource;
    private AceEditor resYamlEditor;
    private String yamlText;
    private String managedFields;
    private Tabs tabs;
    private AceEditor fieldsYaml;
    private XTab tab;
    private UI ui;
    private MenuItem resMenuItemEdit;
    private MenuItem resMenuItemSave;
    private MenuItem resMenuItemCancel;

    public ResourceDetailsPanel(ClusterConfiguration.Cluster clusterConfiguration, CoreV1Api api, MainView mainView, Object resource) {
        this.resource = resource;
    }


    @Override
    public void tabInit(XTab xTab) {
        this.tab = xTab;
        this.ui = UI.getCurrent();

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
        

        resYamlEditor = new AceEditor();
        resYamlEditor.setTheme(AceTheme.terminal);
        resYamlEditor.setMode(AceMode.yaml);
        resYamlEditor.setValue(yamlText);
        resYamlEditor.setReadOnly(true);
        resYamlEditor.setWidthFull();
        resYamlEditor.setHeight("600px"); // TODO setSizeFull()

        var resMenuBar = new MenuBar();
        resMenuItemEdit = resMenuBar.addItem("Edit", e -> {
            resYamlEditor.setReadOnly(false);
            resMenuItemEdit.setEnabled(false);
            resMenuItemSave.setEnabled(true);
            resMenuItemCancel.setEnabled(true);
        });
        resMenuItemSave = resMenuBar.addItem("Save", e -> {
            // TODO
        });
        resMenuItemCancel = resMenuBar.addItem("Cancel", e -> {
            // TODO
        });
        resMenuItemEdit.setEnabled(true);
        resMenuItemSave.setEnabled(false);
        resMenuItemCancel.setEnabled(false);

        var resLayout = new VerticalLayout();
        resLayout.setSizeFull();
        resLayout.add(resMenuBar, resYamlEditor);

        if (managedFields != null) {
            fieldsYaml = new AceEditor();
            fieldsYaml.setTheme(AceTheme.terminal);
            fieldsYaml.setMode(AceMode.yaml);
            fieldsYaml.setValue(managedFields);
            fieldsYaml.setReadOnly(true);
            fieldsYaml.setWidthFull();
            fieldsYaml.setHeight("600px"); // TODO setSizeFull()
        }
        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Yaml", resLayout);
        if (managedFields != null)
            tabSheet.add("Managed Fields", fieldsYaml);

        tabSheet.setSizeFull();
        add(tabSheet);

    }

    @Override
    public void tabSelected() {
        tabRefresh(0);
    }

    @Override
    public void tabUnselected() {

    }

    @Override
    public void tabDestroyed() {

    }

    @Override
    public void tabRefresh(long counter) {
//        if (ui == null || counter % 10 != 0) return;
//        ui.access(() -> {
//            resContainer.getElement().executeJs("return $0.clientHeight", resContainer.getElement()).then( height ->
//            {
//                double h = height.asNumber();
//                yamlRes.setHeight(h + "px");
//                if (yamlFields != null)
//                    yamlFields.setHeight(h + "px");
//            });
//        });
    }

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }
}
