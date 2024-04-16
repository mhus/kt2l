package de.mhus.kt2l.resources;

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.Text;
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
import de.mhus.kt2l.cluster.ClusterConfiguration;
import de.mhus.kt2l.config.AaaConfiguration;
import de.mhus.kt2l.core.SecurityService;
import de.mhus.kt2l.core.SecurityUtils;
import de.mhus.kt2l.core.UiUtil;
import de.mhus.kt2l.k8s.GenericObjectsApi;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.core.MainView;
import de.mhus.kt2l.core.XTab;
import de.mhus.kt2l.core.XTabListener;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class ResourceDetailsPanel extends VerticalLayout implements XTabListener {

    private final String resourceType;
    private final KubernetesObject resource;
    private final CoreV1Api api;
    private AceEditor resYamlEditor;
    private String resContent;
    private String managedFieldsContent;
    private Tabs tabs;
    private AceEditor fieldsYaml;
    private XTab tab;
    private UI ui;
    private MenuItem resMenuItemEdit;
    private MenuItem resMenuItemSave;
    private MenuItem resMenuItemCancel;
    private String statusContent;
    private AceEditor statusYaml;
    private V1APIResource resType;

    @Autowired
    private K8sService k8s;

    @Autowired
    private SecurityService securityService;

    public ResourceDetailsPanel(ClusterConfiguration.Cluster clusterConfiguration, CoreV1Api api, MainView mainView, String resourceType, KubernetesObject resource) {
        this.resourceType = resourceType;
        this.resource = resource;
        this.api = api;
    }


    @Override
    public void tabInit(XTab xTab) {
        this.tab = xTab;
        this.ui = UI.getCurrent();

        resType = k8s.findResource(resourceType, api);
        if (resType == null) {
            UiUtil.showErrorNotification("Unknown resource type");
            return;
        }

        resContent = K8sUtil.toYaml(resource);
        YElement yDocument = MYaml.loadFromString(resContent);

        YMap yMetadata = yDocument.asMap().getMap("metadata");
        YMap yManagedFields = null;
        if (yMetadata != null) {
            yManagedFields = yMetadata.getMap("managedFields");
        }
        if (yManagedFields != null) {
            managedFieldsContent = MYaml.toString(yManagedFields);
            ((Map<String, Object>) yMetadata.getObject()).remove("managedFields");
        }
        YMap yStatus = yDocument.asMap().getMap("status");
        if (yStatus != null) {
            ((Map<String, Object>)yDocument.asMap().getObject()).remove("status");
            statusContent = MYaml.toString(yStatus);
        }

        resContent = MYaml.toString(yDocument);


        resYamlEditor = new AceEditor();
        resYamlEditor.setTheme(AceTheme.terminal);
        resYamlEditor.setMode(AceMode.yaml);
        resYamlEditor.setValue(resContent);
        resYamlEditor.setReadOnly(true);
        resYamlEditor.setWidthFull();
        resYamlEditor.setHeight("600px"); // TODO setSizeFull()

        var resMenuBar = new MenuBar();
        resMenuItemEdit = resMenuBar.addItem("Edit", e -> {
            resYamlEditor.setReadOnly(false);
            resMenuItemEdit.setEnabled(false);
            resMenuItemSave.setEnabled(true);
            resMenuItemCancel.setEnabled(true);
            resYamlEditor.focus();
        });
        resMenuItemSave = resMenuBar.addItem("Save", e -> {
            // TODO save
            try {
                doSave();
            } catch (Exception ex) {
                LOGGER.error("Error saving", ex);
                UiUtil.showErrorNotification("Error saving resource", ex);
                return;
            }

            resYamlEditor.setReadOnly(true);
            resMenuItemEdit.setEnabled(true);
            resMenuItemSave.setEnabled(false);
            resMenuItemCancel.setEnabled(false);

            UiUtil.showSuccessNotification("Resource saved");
        });
        resMenuItemCancel = resMenuBar.addItem("Cancel", e -> {
            resYamlEditor.setValue(resContent);
            resYamlEditor.setReadOnly(true);
            resMenuItemEdit.setEnabled(true);
            resMenuItemSave.setEnabled(false);
            resMenuItemCancel.setEnabled(false);
        });

        resMenuItemEdit.setEnabled(true);
        resMenuItemSave.setEnabled(false);
        resMenuItemCancel.setEnabled(false);

        var resInfo = new Text( "apiVersion: " + toApiVersion(resType) + ", kind: " + resType.getKind());

        var resLayout = new VerticalLayout();
        resLayout.setSizeFull();
        if (securityService.hasRole(AaaConfiguration.SCOPE_RESOURCE_ACTION,SecurityUtils.getResourceId(this) + "_write", "WRITE" ))
            resLayout.add(resMenuBar, resInfo, resYamlEditor);
        else
            resLayout.add(resInfo, resYamlEditor);

        if (statusContent != null) {
            statusYaml = new AceEditor();
            statusYaml.setTheme(AceTheme.terminal);
            statusYaml.setMode(AceMode.yaml);
            statusYaml.setValue(statusContent);
            statusYaml.setReadOnly(true);
            statusYaml.setWidthFull();
            statusYaml.setHeight("600px"); // TODO setSizeFull()
        }

        if (managedFieldsContent != null) {
            fieldsYaml = new AceEditor();
            fieldsYaml.setTheme(AceTheme.terminal);
            fieldsYaml.setMode(AceMode.yaml);
            fieldsYaml.setValue(managedFieldsContent);
            fieldsYaml.setReadOnly(true);
            fieldsYaml.setWidthFull();
            fieldsYaml.setHeight("600px"); // TODO setSizeFull()
        }
        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Yaml", resLayout);
        if (statusContent != null)
            tabSheet.add("Status", statusYaml);
        if (managedFieldsContent != null)
            tabSheet.add("Managed Fields", fieldsYaml);

        UI.getCurrent().getPage().addBrowserWindowResizeListener(e -> {
            resYamlEditor.setHeight( (e.getHeight()-300) + "px");
            if (fieldsYaml != null)
                fieldsYaml.setHeight( (e.getHeight()-250) + "px");
            if (statusYaml != null)
                statusYaml.setHeight( (e.getHeight()-250) + "px");
        });
        UI.getCurrent().getPage().retrieveExtendedClientDetails(details -> {
            resYamlEditor.setHeight( (details.getWindowInnerHeight()- 300) + "px");
            if (fieldsYaml != null)
                fieldsYaml.setHeight( (details.getWindowInnerHeight()-250) + "px");
            if (statusYaml != null)
                statusYaml.setHeight( (details.getWindowInnerHeight()-250) + "px");
        });

        tabSheet.setSizeFull();
        add(tabSheet);

    }

    private void doSave() throws ApiException, IOException {
        var yaml = resYamlEditor.getValue();

        yaml = "apiVersion: " + toApiVersion(resType) + "\nkind: " + resType.getKind() + "\n" + yaml;

        var handler = k8s.getResourceHandler(resType.getKind());

        if (handler == null) {
            var yamlObj = Yaml.loadAs(yaml, V1Pod.class);
            GenericObjectsApi genericObjectsApi = new GenericObjectsApi(api.getApiClient());
            genericObjectsApi.patchClusterCustomObject(
                    resType.getGroup(),
                    resType.getVersion() == null ? "v1" : resType.getVersion(),
                    resType.getName(),
                    null,
                    yamlObj,
                    null,
                    managedFieldsContent,
                    false
            );

        } else {
            handler.replace(api, resource.getMetadata().getName(), resource.getMetadata().getNamespace(), yaml);
        }

    }

    private String toApiVersion(V1APIResource resType) {
        if (resType.getGroup() == null && resType.getVersion() == null)
            return "v1";
        if (resType.getGroup() == null)
            return resType.getVersion();
        return resType.getGroup() + "/" + resType.getVersion();
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
