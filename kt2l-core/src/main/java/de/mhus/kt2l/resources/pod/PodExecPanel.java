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

package de.mhus.kt2l.resources.pod;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.config.ShellConfiguration;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.ui.UiUtil;
import de.mhus.kt2l.help.HelpResourceConnector;
import de.mhus.kt2l.k8s.ApiProvider;
import de.mhus.kt2l.kscript.Block;
import de.mhus.kt2l.kscript.RunCompiler;
import de.mhus.kt2l.kscript.RunContext;
import de.mhus.kt2l.resources.util.ResourceSelector;
import de.mhus.kt2l.storage.StorageFile;
import de.mhus.kt2l.storage.StorageService;
import io.azam.ulidj.ULID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static de.mhus.commons.tools.MLang.tryThis;

@Configurable
@Slf4j
public class PodExecPanel extends VerticalLayout implements DeskTabListener, HelpResourceConnector {

    @Autowired
    private ShellConfiguration shellConfiguration;
    @Autowired
    private StorageService storageService;

    private final ApiProvider apiProvider;
    private final ResourceSelector<ContainerResource> resourceSelector;
    private final Cluster cluster;
    private final Core core;
    private DeskTab tab;
    private AceEditor editor;
    private VerticalLayout results;
    private MenuItem menuItemRun;
    private List<ResultEntry> resultList = Collections.synchronizedList(new LinkedList<>());
    private MenuItem menuItemClear;
    private MenuItem menuItemStop;
    private MenuItem menuItemStore;
    private MenuItem menuItemCapture;
    private Div menuItemStoreIconDiv;
    private volatile StorageFile captureDirectory;
    private Icon menuItemStoreIconDivIcon;

    public PodExecPanel(Cluster cluster, Core core, List<ContainerResource> containers) {
        this.apiProvider = cluster.getApiProvider();
        this.resourceSelector = new ResourceSelector(containers, true);
        this.cluster = cluster;
        this.core = core;
    }

    @Override
    public void tabInit(DeskTab deskTab) {
        this.tab = deskTab;

        var menuBar = new MenuBar();
        resourceSelector.injectMenu(menuBar);
        menuItemRun = menuBar.addItem("Run", e -> {
            menuItemRun.setEnabled(false);
            menuItemStop.setEnabled(true);
            editor.setReadOnly(true);
            try {
                runCommand();
            } catch (Exception ex) {
                //XXX
                LOGGER.error("Run", ex);
            }
        });
        menuItemStop = menuBar.addItem("Stop", e -> {
            for (ResultEntry entry : resultList) {
                if (entry.thread.isAlive()) {
                    entry.context.close();
                    entry.thread.interrupt();
                }
            }
            menuItemRun.setEnabled(true);
            menuItemStop.setEnabled(false);
            editor.setReadOnly(false);
        });
        menuItemClear = menuBar.addItem("Clear", e -> {
            resultList.removeIf(r -> {
                if (r.thread.isAlive())
                    return false;
                results.remove(r.text);
                return true;
            });
        });

        if (storageService.isEnabled()) {
            captureDirectory = tryThis(() -> storageService.getStorage().createDirectory("exec")).or(null);
            if (captureDirectory != null) {
                menuItemStoreIconDivIcon = VaadinIcon.BULLSEYE.create();
                menuItemStoreIconDivIcon.setVisible(false);
                menuItemStoreIconDivIcon.addClassName("color-red");
                menuItemStoreIconDivIcon.setSize("var(--lumo-icon-size-s)");
                menuItemStoreIconDiv = new Div();
                menuItemStoreIconDiv.add(menuItemStoreIconDivIcon, new Text(" Store"));
                menuItemStore = menuBar.addItem(menuItemStoreIconDiv);
                var storeMenu = menuItemStore.getSubMenu();
                menuItemCapture = storeMenu.addItem("Capture", e -> {
//                    menuItemCapture.setChecked(!menuItemCapture.isChecked());
                    menuItemStoreIconDivIcon.setVisible(menuItemCapture.isChecked());
                });
                menuItemCapture.setCheckable(true);
                captureDirectory = tryThis(() -> storageService.getStorage().createDirectory("exec")).or(null);

                storeMenu.addItem(captureDirectory.getName(), e -> {
                    storageService.showStoragePanel(core, captureDirectory);
                });
            }
        }

        
        editor = new AceEditor();
        editor.setTheme(AceTheme.terminal);
        editor.setMode(AceMode.sh);
        editor.setAutoComplete(true);
        editor.addStaticWordCompleter(List.of("exec", "wait", "sleep", "echo", "send", "close", "clear", "set", "env"));
        editor.setValue("exec cmd=\"ls -la\"\nwait");
        editor.setWidthFull();
        // editor.setHeight("200px");

        var editorLayout = new VerticalLayout();
        editorLayout.add(editor);
        editorLayout.setWidthFull();

        results = new VerticalLayout();
        results.setWidthFull();

        SplitLayout splitLayout = new SplitLayout(editorLayout, results);
        splitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
        splitLayout.setSplitterPosition(50);
        splitLayout.setSizeFull();

        setSizeFull();
        setPadding(false);
        setMargin(false);

        add(menuBar);
        add(splitLayout);

    }

    private void runCommand() throws Exception {
        String command = editor.getValue();

        var compiledBlock = compileProgram(command);

        for (ContainerResource container : resourceSelector.getResources()) {

            var text = new TextArea();
            text.setLabel(container.getContainerName());
            text.addClassName("bgcolor-yellow");
            text.setWidthFull();
            text.setValue("Running ...");
            var menu = new ContextMenu();
            menu.setTarget(text);
            menu.addItem("Stop", e -> {
                for (ResultEntry entry : resultList) {
                    if (entry.container.equals(container)) {
                        if (entry.thread.isAlive()) {
                            entry.context.close();
                            entry.thread.interrupt();
                        }
                        return;
                    }
                }
            });
            menu.addItem("Clear", e -> {
                resultList.removeIf(r -> {
                    if (r.container.equals(container) && !r.thread.isAlive()) {
                        results.remove(r.text);
                        return true;
                    }
                    return false;
                });
            });

            results.add(text);

            RunContext context = new RunContext();

            var thread = Thread.startVirtualThread(() -> {
                runCommandInContainer(container, compiledBlock, text, context);
            });

            resultList.add(new ResultEntry(container, text, context, thread));

        }
    }

    private Block compileProgram(String command) throws Exception {
        return new RunCompiler().compile(command);
    }

    private void runCommandInContainer(ContainerResource container, Block compiledBlock, TextArea text, RunContext context) {
        try (var sce = context.getSecurityContext().enter()) {
            context.getProperties().setString(RunCompiler.PROP_SHELL, shellConfiguration.getShellFor(cluster, container.getPod()));
            context.getProperties().setString(RunCompiler.PROP_CONTAINER, container.getContainerName());
            context.setTextChangedObserver(s -> {
                core.ui().access(() -> {
                    text.setValue(s);
                });
            });
            context.setApiProvider(apiProvider);
            context.setPod(container.getPod());

            compiledBlock.run(context, null);

            core.ui().access(() -> {
                text.removeClassNames("bgcolor-yellow");
            });
        } catch (Exception e) {
            LOGGER.error("Execute", e);
            core.ui().access(() -> {
                text.removeClassNames("bgcolor-yellow");
                text.addClassName("bgcolor-red");
                text.setValue(text.getValue() + "\n" + e.getMessage());
            });
        }
        if (menuItemCapture.isChecked()) {
            try (var stream = captureDirectory.getStorage().createFileStream(captureDirectory, ULID.random() + "-" + container.getContainerName() + ".cap.log").getStream()) {
                OutputStreamWriter writer = new OutputStreamWriter(stream);
                writer.write(text.getValue());
                writer.flush();
            } catch (IOException e) {
                LOGGER.error("Capture", e);
                core.ui().access(() -> UiUtil.showErrorNotification("Capture " + e.getMessage()));
            }
        }
    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabUnselected() {

    }

    @Override
    public void tabDestroyed() {
        resultList.forEach(r -> {
            r.context.close();
            r.thread.interrupt();
        });
        resultList.clear();
    }

    @Override
    public void tabRefresh(long counter) {
        if (!menuItemRun.isEnabled()) {
            for (ResultEntry entry : resultList) {
                if (entry.thread.isAlive()) {
                    return;
                }
            }
            core.ui().access(() -> {
                menuItemRun.setEnabled(true);
                menuItemStop.setEnabled(false);
                editor.setReadOnly(false);
            });
        }
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

    private record ResultEntry(ContainerResource container, TextArea text, RunContext context, Thread thread) {
    }
}
