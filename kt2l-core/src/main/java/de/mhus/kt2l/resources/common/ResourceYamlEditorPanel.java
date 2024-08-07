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

package de.mhus.kt2l.resources.common;

import com.vaadin.flow.component.Text;
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
import de.mhus.kt2l.aaa.AaaConfiguration;
import de.mhus.kt2l.aaa.SecurityService;
import de.mhus.kt2l.aaa.SecurityUtils;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.help.HelpResourceConnector;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1APIResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class ResourceYamlEditorPanel extends VerticalLayout implements DeskTabListener, HelpResourceConnector {

    private final ApiProvider apiProvider;
    private final V1APIResource type;
    private KubernetesObject resource;
    private final Core core;
    private final Cluster cluster;
    private AceEditor resYamlEditor;
    private String resContent;
    private String managedFieldsContent;
    private Tabs tabs;
    private AceEditor fieldsYaml;
    private DeskTab tab;
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
    private MenuItem resMenuItemReload;

    public ResourceYamlEditorPanel(Cluster cluster, Core core, V1APIResource type, KubernetesObject resource) {
        this.apiProvider = cluster.getApiProvider();
        this.type = type;
        this.resource = resource;
        this.core = core;
        this.cluster = cluster;
    }


    @Override
    public void tabInit(DeskTab deskTab) {
        this.tab = deskTab;

        resType = k8s.findResource(type, apiProvider);
        if (resType == null) {
            UiUtil.showErrorNotification("Unknown resource type");
            return;
        }

        setContent();

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
            resMenuItemReload.setEnabled(false);
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
            resMenuItemReload.setEnabled(true);

            UiUtil.showSuccessNotification("Resource saved");

            doReload();
        });
        resMenuItemCancel = resMenuBar.addItem("Cancel", e -> {
            resYamlEditor.setValue(resContent);
            resYamlEditor.setReadOnly(true);
            resMenuItemEdit.setEnabled(true);
            resMenuItemSave.setEnabled(false);
            resMenuItemCancel.setEnabled(false);
            resMenuItemReload.setEnabled(true);
        });
        resMenuItemReload = resMenuBar.addItem("Reload", e -> {
            doReload();
        });

        resMenuItemEdit.setEnabled(true);
        resMenuItemSave.setEnabled(false);
        resMenuItemCancel.setEnabled(false);
        resMenuItemReload.setEnabled(true);

        var resInfo = new Text( "apiVersion: " + toApiVersion(resType) + ", kind: " + resType.getKind());

        var resLayout = new VerticalLayout();
        resLayout.setSizeFull();
        resLayout.setPadding(false);
        resLayout.setMargin(false);
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

        core.ui().getPage().addBrowserWindowResizeListener(e -> {
            resYamlEditor.setHeight( (e.getHeight()-300) + "px");
            if (fieldsYaml != null)
                fieldsYaml.setHeight( (e.getHeight()-250) + "px");
            if (statusYaml != null)
                statusYaml.setHeight( (e.getHeight()-250) + "px");
        });
        core.ui().getPage().retrieveExtendedClientDetails(details -> {
            resYamlEditor.setHeight( (details.getWindowInnerHeight()- 300) + "px");
            if (fieldsYaml != null)
                fieldsYaml.setHeight( (details.getWindowInnerHeight()-250) + "px");
            if (statusYaml != null)
                statusYaml.setHeight( (details.getWindowInnerHeight()-250) + "px");
        });

        tabSheet.setSizeFull();
        add(tabSheet);

        setSizeFull();
        setPadding(false);
        setMargin(false);
    }

    private void doReload() {
        var handler = k8s.getTypeHandler(resType);
        if (handler == null) {
            UiUtil.showErrorNotification("No handler found");
            return;
        }
        try {
            resource = handler.get(apiProvider, resource.getMetadata().getName(), resource.getMetadata().getNamespace());
            setContent();
            resYamlEditor.setValue(resContent);
        } catch (ApiException ex) {
            LOGGER.debug("Error reloading", ex);
            UiUtil.showErrorNotification("Error reloading resource", ex);
        }
    }

    private void setContent() {
        resContent = K8sUtil.toYamlString(resource);
        YElement yDocument = MYaml.loadFromString(resContent);

        YMap yMetadata = yDocument.asMap().getMap("metadata");
        YMap yManagedFields = null;
        if (yMetadata != null) {
            yManagedFields = yMetadata.getMap("managedFields");
        }
        if (yManagedFields != null) {
            managedFieldsContent = MYaml.toString(yManagedFields);
            //noinspection unchecked
            ((Map<String, Object>) yMetadata.getObject()).remove("managedFields");
        }
        YMap yStatus = yDocument.asMap().getMap("status");
        if (yStatus != null) {
            //noinspection unchecked
            ((Map<String, Object>)yDocument.asMap().getObject()).remove("status");
            statusContent = MYaml.toString(yStatus);
        }

        resContent = MYaml.toString(yDocument);
    }

    private void doSave() throws ApiException, IOException {
        var yaml = resYamlEditor.getValue();

        yaml = "apiVersion: " + toApiVersion(resType) + "\nkind: " + resType.getKind() + "\n" + yaml;

        var handler = k8s.getTypeHandler(resType);

        if (handler == null) {
//XXX
        } else {
            handler.replace(apiProvider, resource.getMetadata().getName(), resource.getMetadata().getNamespace(), yaml);
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
    }

    @Override
    public String getHelpContent() {
        if (resYamlEditor.isReadOnly())
            return null;
        return
            "kind: " + resType.getKind() + "\n" +
            "apiVersion: " + resType.getVersion() + "\n"
            + resYamlEditor.getValue();
    }

    @Override
    public void setHelpContent(String content) {
        content = content.replaceAll("kind: " + resType.getKind() + "\n", "");
        content = content.replaceAll("apiVersion:.*\n", "");
        final var contentFinal = content;
        getUI().get().access(() -> {
            resYamlEditor.setValue(contentFinal);
        });
    }

    @Override
    public int getHelpCursorPos() {
        return resYamlEditor.getCursorPosition().getIndex();
    }
}
