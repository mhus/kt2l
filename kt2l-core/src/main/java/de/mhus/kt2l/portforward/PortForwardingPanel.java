package de.mhus.kt2l.portforward;

import com.vaadin.componentfactory.ToggleButton;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.core.UiUtil;
import de.mhus.kt2l.help.HelpResourceConnector;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PortForwardingPanel extends VerticalLayout implements DeskTabListener, HelpResourceConnector {
    private final Core core;
    private final Cluster cluster;
    private PortForwardBackgroundJob portForwarder;
    private TextArea command;
    private VerticalLayout forwardings;

    public PortForwardingPanel(Core core, Cluster cluster) {
        this.core = core;
        this.cluster = cluster;
    }

    @Override
    public void tabInit(DeskTab deskTab) {
        portForwarder = core.backgroundJobInstance(cluster, PortForwardBackgroundJob.class);

        var menuBar = new MenuBar();
        menuBar.addItem("Add", e -> addForwarding());
        menuBar.addItem("Start All", e -> {
            portForwarder.getForwardings().forEach(f -> f.start());
            tabRefresh(0);
        });
        menuBar.addItem("Stop All", e -> {
            portForwarder.getForwardings().forEach(f -> f.stop());
            tabRefresh(0);
        });
        menuBar.addItem("Remove All", e -> {
            portForwarder.getForwardings().forEach(f -> portForwarder.removeForwarding(f));
            forwardings.removeAll();
            tabRefresh(0);
        });
        add(menuBar);

        command = new TextArea();
        command.setLabel("Port Forwardings");
        command.setWidthFull();
        command.setHeight("200px");
        add(command);

        forwardings = new VerticalLayout();
        forwardings.setSizeFull();
        forwardings.setPadding(false);
        forwardings.setMargin(false);
        forwardings.setSpacing(false);
        forwardings.addClassName("forwardings");
        add(forwardings);

        portForwarder.getForwardings().forEach(f -> forwardings.add(new ForwardEntry(f)));

    }

    private void addForwarding() {
        try {
            var cmds = command.getValue();
//            var selectionStart = command.getElement().executeJs("return this.inputElement.selectionStart").toCompletableFuture().get();
//            if (selectionStart.getType() == JsonType.NUMBER) {
//                var selectionEnd = command.getElement().executeJs("return this.inputElement.selectionEnd").toCompletableFuture().get();
//                cmds = cmds.substring((int)selectionStart.asNumber(), (int)selectionEnd.asNumber());
//            }

            var parts = cmds.split("\n");
            for (String cmd : parts) {
                var p = cmd.split("\\s+");
                if (p.length < 5) {
                    UiUtil.showErrorNotification("Invalid command: " + cmd);
                    continue;
                }
                var cmdName = p[0].trim();
                var namespace = p[1].trim();
                var name = p[2].trim();
                var remotePort = Integer.parseInt(p[3]);
                var localPort = Integer.parseInt(p[4]);
                var action = p.length > 5 ? p[5].trim() : null;
                if ("pod".equalsIgnoreCase(cmdName) || "svc".equalsIgnoreCase(cmdName)) {
                    try {
                        var f = portForwarder.getForwarding(cmdName.toLowerCase(), namespace, name, remotePort, localPort)
                                .orElseGet(() -> {
                                    var newF = "pod".equalsIgnoreCase(cmdName) ?
                                            portForwarder.addPodForwarding(namespace, name, remotePort, localPort)
                                            :
                                            portForwarder.addServiceForwarding(namespace, name, remotePort, localPort)
                                            ;
                                    forwardings.add(new ForwardEntry(newF));
                                    return newF;
                                });

                        if (action != null) {
                            if ("on".equalsIgnoreCase(action)) {
                                f.start();
                            } else if ("off".equalsIgnoreCase(action)) {
                                f.stop();
                            } else {
                                UiUtil.showErrorNotification("Invalid action: " + action);
                            }
                            tabRefresh(0);
                        }
                    } catch (Exception e) {
                        UiUtil.showErrorNotification("Error: " + e.getMessage());
                    }
                } else {
                    UiUtil.showErrorNotification("Invalid command: " + cmdName);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error", e);
            UiUtil.showErrorNotification("Error: " + e.getMessage());
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

    }

    @Override
    public synchronized void tabRefresh(long counter) {
        if (counter % 10 == 0) {
            forwardings.getChildren().forEach(c -> {
                if (c instanceof ForwardEntry) {
                    var f = ((ForwardEntry) c).forwarding;
                    if (f.isClosed()) {
                        core.ui().access(() -> {
                            forwardings.remove(c);
                        });
                        return;
                    }
                    core.ui().access(() -> {
                        ((ForwardEntry) c).toggle.setValue(f.isRunning());
                        ((ForwardEntry) c).stats.setText("CON:" + f.currentConnections() + " TX:" + MString.toByteDisplayString(f.getTx()) + " RX:" + MString.toByteDisplayString(f.getRx()) );
                    });
                }
            });
        }
    }

    public void setCommand(String cmd) {
        command.setValue(cmd);
    }

    @Override
    public String getHelpContent() {
        return command.getValue();
    }

    @Override
    public void setHelpContent(String content) {
        command.setValue(content);
    }

    @Override
    public int getHelpCursorPos() {
        return -1;
    }

    private class ForwardEntry extends HorizontalLayout {
        private final PortForwardBackgroundJob.Forwarding forwarding;
        private final ToggleButton toggle;
        private final Div title;
        private final Div stats;

        public ForwardEntry(PortForwardBackgroundJob.Forwarding f) {
            this.forwarding = f;
            setWidthFull();
            setPadding(false);
            setMargin(false);

            toggle = new ToggleButton();
            toggle.setValue(forwarding.isRunning());
            toggle.addValueChangeListener(e -> {
                if (e.getValue()) {
                    forwarding.start();
                } else {
                    forwarding.stop();
                }
            });
            title = new Div(forwarding.toString());
            title.setWidth("50%");
            stats = new Div();
            stats.setWidth("50%");
            var removeBtn = new Button(VaadinIcon.CLOSE.create());
            removeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            removeBtn.addClickListener(e -> {
                forwardings.remove(this);
                portForwarder.removeForwarding(forwarding);
            });
            add(toggle, title, stats, removeBtn);

        }
    }
}
