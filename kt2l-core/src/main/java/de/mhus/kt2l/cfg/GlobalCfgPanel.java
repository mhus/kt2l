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
package de.mhus.kt2l.cfg;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import de.mhus.commons.tree.MTree;
import de.mhus.kt2l.Kt2lApplication;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.ui.UiUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class GlobalCfgPanel extends VerticalLayout {

    private final Core core;
    private final boolean isGlobalConfig;
    private final List<CfgFactory> factories;
    private final File configDir;
    private final File[] fallbackDirs;
    private final List<PanelStore> panels = new LinkedList<>();

    public GlobalCfgPanel(Core core, boolean isGlobalConfig, List<CfgFactory> factories, File configDir, File ... fallbackDirs) {
        this.core = core;
        this.isGlobalConfig = isGlobalConfig;
        this.factories = factories;
        this.configDir = configDir;
        this.fallbackDirs = fallbackDirs;

        initUi();
        load();
    }

    private void save() {
        panels.forEach(ps -> {
            try {
                var file = new File(configDir, ps.factory().handledConfigType() + ".yaml");
                var actualFile = findActualConfigFile(ps.factory().handledConfigType());
                var content = actualFile != null ? MTree.load(actualFile) : MTree.create();

                ps.panel().save(content);
                MTree.save(content, file);
            } catch (Exception t) {
                UiUtil.showErrorNotification("Can't save panel " + ps.factory().handledConfigType());
                LOGGER.error("Can't save panel {}", ps.factory().handledConfigType(), t);
            }
        });

        if (isGlobalConfig) {
            UiUtil.showSuccessNotification("Saved configuration");
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Restart required");
            if (Kt2lApplication.canRestart()) {
                dialog.setText("The configuration has been saved. A restart of the server is required to apply the changes.");
                dialog.setConfirmText("Restart");
            } else {
                dialog.setText("The configuration has been saved. A restart is required to apply the changes. Please start the application again.");
                dialog.setConfirmText("Shutdown");
            }
            dialog.setCloseOnEsc(true);
            dialog.addConfirmListener(e -> Kt2lApplication.restart());
            dialog.open();
        } else {
            UiUtil.showSuccessNotification("Saved configuration");
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Reset session required");
            dialog.setText("The configuration has been saved. A reset of the current session is required to apply the changes.");
            dialog.setConfirmText("Reset session");
            dialog.setCloseOnEsc(true);
            dialog.addConfirmListener(e -> core.resetSession());
            dialog.open();
        }
    }

    private void load() {
        panels.forEach(ps -> {
            try {
                var file = findActualConfigFile(ps.factory().handledConfigType());
                if (file != null) {
                    var content = MTree.load(file);
                    ps.panel().load(content);
                }
            } catch (Exception t) {
                LOGGER.error("Can't load panel {}", ps.factory().handledConfigType(), t);
            }
        });
    }

    private File findActualConfigFile(String handledConfigType) {
        var file = new File(configDir, handledConfigType + ".yaml");
        if (file.exists()) {
            LOGGER.debug("Load config {} from {}", handledConfigType, file);
            return file;
        } else if (fallbackDirs != null) {
            for (File fallbackDir : fallbackDirs) {
                var fallbackFile = new File(fallbackDir, handledConfigType + ".yaml");
                if (fallbackFile.exists()) {
                    LOGGER.debug("Load config {} from {}", handledConfigType, fallbackFile);
                    return fallbackFile;
                }
            }
        }
        LOGGER.debug("Load config {} not found", handledConfigType);
        return null;
    }

    public void initUi() {
        TabSheet tabSheet = new TabSheet();

        for (CfgFactory factory : factories) {
            var panel = factory.createPanel();
            UiUtil.autowireBean(core, panel);
            panel.initUi();
            tabSheet.add(panel.getTitle(), panel.getPanel());
            panels.add(new PanelStore(panel, factory));
        }

        tabSheet.setWidthFull();
        add(tabSheet);

        MenuBar menuBar = new MenuBar();
        menuBar.addItem("Save Settings", e -> save());
        add(menuBar);

        var spacer = new Div(" ");
        spacer.setHeight("30px");
        add(spacer);

        setMargin(false);
        setPadding(false);
    }

    private record PanelStore(CfgPanel panel, CfgFactory factory) {
    }

}
