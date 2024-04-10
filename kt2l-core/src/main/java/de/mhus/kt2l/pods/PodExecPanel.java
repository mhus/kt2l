package de.mhus.kt2l.pods;

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import de.mhus.kt2l.cluster.ClusterConfiguration;
import de.mhus.kt2l.config.ConfigUtil;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.kscript.Block;
import de.mhus.kt2l.kscript.RunCompiler;
import de.mhus.kt2l.kscript.RunContext;
import de.mhus.kt2l.resources.ResourceManager;
import de.mhus.kt2l.ui.MainView;
import de.mhus.kt2l.ui.XTab;
import de.mhus.kt2l.ui.XTabListener;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class PodExecPanel extends VerticalLayout implements XTabListener  {

    @Autowired
    private Configuration configuration;

    private final ResourceManager<ContainerResource> resourceManager;
    private final ClusterConfiguration.Cluster clusterConfig;
    private final CoreV1Api api;
    private final MainView mainView;
    private final UI ui;
    private XTab tab;
    private AceEditor editor;
    private VerticalLayout results;
    private MenuItem menuItemRun;
    private List<ResultEntry> resultList = Collections.synchronizedList(new LinkedList<>());
    private MenuItem menuItemClear;
    private MenuItem menuItemStop;

    public PodExecPanel(ClusterConfiguration.Cluster clusterConfig, CoreV1Api api, MainView mainView, List<ContainerResource> containers) {
        this.resourceManager = new ResourceManager(containers, true);
        this.clusterConfig = clusterConfig;
        this.api = api;
        this.mainView = mainView;
        this.ui = UI.getCurrent();
    }

    @Override
    public void tabInit(XTab xTab) {
        this.tab = xTab;

        var menuBar = new MenuBar();
        resourceManager.injectMenu(menuBar);
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

        add(menuBar);
        add(splitLayout);

    }

    private void runCommand() throws Exception {
        String command = editor.getValue();

        var compiledBlock = compileProgram(command);

        for (ContainerResource container : resourceManager.getResources()) {

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
        try {
            context.getProperties().setString(RunCompiler.PROP_SHELL, ConfigUtil.getShellFor(configuration, clusterConfig, container.getPod()));
            context.getProperties().setString(RunCompiler.PROP_CONTAINER, container.getContainerName());
            context.setTextChangedObserver(s -> {
                ui.access(() -> {
                    text.setValue(s);
                });
            });
            context.setApi(api);
            context.setPod(container.getPod());

            compiledBlock.run(context, null);

            ui.access(() -> {
                text.removeClassNames("bgcolor-yellow");
            });
        } catch (Exception e) {
            LOGGER.error("Execute", e);
            ui.access(() -> {
                text.removeClassNames("bgcolor-yellow");
                text.addClassName("bgcolor-red");
                text.setValue(text.getValue() + "\n" + e.getMessage());
            });
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
            ui.access(() -> {
                menuItemRun.setEnabled(true);
                menuItemStop.setEnabled(false);
                editor.setReadOnly(false);
            });
        }
    }

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }

    private record ResultEntry(ContainerResource container, TextArea text, RunContext context, Thread thread) {
    }
}
