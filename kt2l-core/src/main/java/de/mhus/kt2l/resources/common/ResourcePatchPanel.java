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

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.ui.ProgressDialog;
import de.mhus.kt2l.ui.UiUtil;
import de.mhus.kt2l.help.HelpResourceConnector;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.resources.util.ResourceSelector;
import io.kubernetes.client.common.KubernetesObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.Set;

@Configurable
@Slf4j
public class ResourcePatchPanel extends VerticalLayout implements DeskTabListener, HelpResourceConnector {
    private final Cluster cluster;
    private final Core core;
    private DeskTab tab;
    private AceEditor editor;
    private final ResourceSelector<KubernetesObject> resourceSelector;

    @Autowired
    private K8sService k8s;

    public ResourcePatchPanel(Cluster cluster, Core core, Set<? extends KubernetesObject> selected) {
        this.cluster = cluster;
        this.core = core;
        this.resourceSelector = new ResourceSelector(selected, true);
    }

    @Override
    public void tabInit(DeskTab deskTab) {
        this.tab = deskTab;

        var menuBar = new MenuBar();
        resourceSelector.injectMenu(menuBar);
        var patchItem = menuBar.addItem(VaadinIcon.FORWARD.create(), e -> {
            patchResource();
        });
        patchItem.add("Patch");
        add(menuBar);

        editor = new AceEditor();
        editor.setTheme(AceTheme.terminal);
        editor.setMode(AceMode.yaml);
        editor.setValue("[{\"op\":\"replace\",\"path\":\"/spec/\",\"value\":\n\n}]");
        editor.setReadOnly(false);
        editor.setSizeFull();

        add(editor);
        setSizeFull();
        setPadding(false);
        setMargin(false);

    }

    private void patchResource() {

        ProgressDialog dialog = new ProgressDialog();
        dialog.setHeaderTitle("Patch Resources");
        dialog.setMax(resourceSelector.size());
        dialog.open();

        Thread.startVirtualThread(() -> {
            for (KubernetesObject res : resourceSelector.getResources()) {
                core.ui().access(() -> {
                    try {
                        dialog.next(res.getMetadata().getName());
                        var content = editor.getValue();
                        var handler = k8s.getTypeHandler(K8sUtil.toType(res, cluster));
                        handler.patch(cluster.getApiProvider(), res, content);
                        UiUtil.showSuccessNotification("Resource patched: " + res.getMetadata().getNamespace() + "." + res.getMetadata().getName());
                    } catch (Exception t) {
                        LOGGER.error("Error patching resource {}", res.getMetadata().getName(), t);
                        UiUtil.showErrorNotification("Error creating resource", t);
                    }
                });
            }
            core.ui().access(() -> {
                dialog.close();
            });
        });
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

    }

    @Override
    public String getHelpContent() {
        return editor.getValue();
    }

    @Override
    public void setHelpContent(String content) {
        editor.setValue(content);
    }

    @Override
    public int getHelpCursorPos() {
        return editor.getCursorPosition().getIndex();
    }

}
