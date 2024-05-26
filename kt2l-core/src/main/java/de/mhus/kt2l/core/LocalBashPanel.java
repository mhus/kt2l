package de.mhus.kt2l.core;

import com.flowingcode.vaadin.addons.xterm.ITerminalClipboard;
import com.flowingcode.vaadin.addons.xterm.ITerminalOptions;
import com.flowingcode.vaadin.addons.xterm.XTerm;
import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.commons.tools.MJson;
import de.mhus.commons.tools.MLang;
import de.mhus.commons.tools.MThread;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class LocalBashPanel extends VerticalLayout implements DeskTabListener {
    private final Core core;
    private XTerm xterm;
    private MenuItem menuItemEsc;
    private volatile Process proc;
    private volatile boolean echoOn = true;

    public LocalBashPanel(Core core) {
        this.core = core;
    }

    @Override
    public void tabInit(DeskTab deskTab) {

        xterm = new XTerm();
        xterm.writeln("Start console\n\n");
        xterm.setCursorBlink(true);
        xterm.setCursorStyle(ITerminalOptions.CursorStyle.UNDERLINE);

        xterm.setSizeFull();
        xterm.setCopySelection(true);
        xterm.setUseSystemClipboard(ITerminalClipboard.UseSystemClipboard.READWRITE);
        xterm.setPasteWithRightClick(true);
        xterm.addAnyKeyListener(e -> {
            System.out.println("Key: " + e.getKey());
            try {
                /*
Key: {"key":"A","code":"KeyA","ctrlKey":false,"altKey":false,"metaKey":false,"shiftKey":true}
Key: {"key":"Meta","code":"MetaLeft","ctrlKey":false,"altKey":false,"metaKey":true,"shiftKey":false}
Key: {"key":"ArrowUp","code":"ArrowUp","ctrlKey":false,"altKey":false,"metaKey":false,"shiftKey":false}
Key: {"key":"Meta","code":"MetaLeft","ctrlKey":false,"altKey":false,"metaKey":true,"shiftKey":false}
Key: {"key":"Meta","code":"MetaLeft","ctrlKey":false,"altKey":false,"metaKey":true,"shiftKey":false}
Key: {"key":"Escape","code":"Escape","ctrlKey":false,"altKey":false,"metaKey":false,"shiftKey":false}
Key: {"key":"Meta","code":"MetaLeft","ctrlKey":false,"altKey":false,"metaKey":true,"shiftKey":false}
                 */
                var json = MJson.load(e.getKey());
                var key = json.get("key").asText();
                if (key.length() == 1) {
                    if (json.get("ctrlKey").asBoolean()) {
                        key = key.toUpperCase();
                        if (key.equals("C"))
                            proc.getOutputStream().write("\u0003".getBytes());
                        else
                        if (key.equals("D"))
                            proc.getOutputStream().write("\u0004".getBytes());
                        else
                        if (key.equals("Z"))
                            proc.getOutputStream().write("\u001a".getBytes());
                        else if (key.equals("V"))
                            proc.getOutputStream().write("\u0016".getBytes());
                        else if (key.equals("X"))
                            proc.getOutputStream().write("\u0018".getBytes());
                        else if (key.equals("A"))
                            proc.getOutputStream().write("\u0001".getBytes());
                        else if (key.equals("E"))
                            proc.getOutputStream().write("\u0005".getBytes());
                        else if (key.equals("K"))
                            proc.getOutputStream().write("\u000b".getBytes());
                        else if (key.equals("L"))
                            proc.getOutputStream().write("\u000c".getBytes());
                        else if (key.equals("U"))
                            proc.getOutputStream().write("\u0015".getBytes());
                        else if (key.equals("W"))
                            proc.getOutputStream().write("\u0017".getBytes());
                        else if (key.equals("Y"))
                            proc.getOutputStream().write("\u0019".getBytes());
                        else if (key.equals("N"))
                            proc.getOutputStream().write("\u000e".getBytes());
                        else if (key.equals("P"))
                            proc.getOutputStream().write("\u0010".getBytes());
                        else if (key.equals("R"))
                            proc.getOutputStream().write("\u0012".getBytes());
                        else if (key.equals("T"))
                            proc.getOutputStream().write("\u0014".getBytes());
                        else if (key.equals("F"))
                            proc.getOutputStream().write("\u0006".getBytes());
                        else if (key.equals("B"))
                            proc.getOutputStream().write("\u0002".getBytes());
                        else if (key.equals("M"))
                            proc.getOutputStream().write("\r".getBytes());
                        else if (key.equals("S"))
                            proc.getOutputStream().write("\u0013".getBytes());
                        else if (key.equals("H"))
                            proc.getOutputStream().write("\u0008".getBytes());
                        else if (key.equals("J"))
                            proc.getOutputStream().write("\n".getBytes());
                        else if (key.equals("G"))
                            proc.getOutputStream().write("\u0007".getBytes());
                        else if (key.equals("I"))
                            proc.getOutputStream().write("\t".getBytes());
                        else if (key.equals("O"))
                            proc.getOutputStream().write("\u000f".getBytes());
                        else if (key.equals("Q"))
                            proc.getOutputStream().write("\u0011".getBytes());
                        return;
                    }

                    try {
                        proc.getOutputStream().write(key.getBytes());
                        if (echoOn) {
                            final var finalKey = key;
                            core.ui().access(() -> xterm.write(new String(finalKey.getBytes())));
                        }
                        LOGGER.info("Alive: {}", proc.isAlive());
                    } catch (IOException ex) {
                        LOGGER.error("Write error", ex);
                        closeTerminal();
                    }
                } else
                if (key.equals("Escape")) {
                    proc.getOutputStream().write("\u001b".getBytes());
                } else
                if (key.equals("ArrowUp")) {
                    proc.getOutputStream().write("\u001b[A".getBytes());
                }
                else if (key.equals("ArrowDown")) {
                    proc.getOutputStream().write("\u001b[B".getBytes()
                    );
                }
                else if (key.equals("ArrowRight")) {
                    proc.getOutputStream().write("\u001b[C".getBytes());
                }
                else if (key.equals("ArrowLeft")) {
                    proc.getOutputStream().write("\u001b[D".getBytes());
                }
                else if (key.equals("Backspace")) {
                    proc.getOutputStream().write("\u007f".getBytes());
                }
                else if (key.equals("Delete")) {
                    proc.getOutputStream().write("\u001b[3~".getBytes());
                }
                else if (key.equals("Enter")) {
                    proc.getOutputStream().write("\n".getBytes());
                    if (echoOn) {
                        core.ui().access(() -> xterm.write("\n"));
                    }
                } else if (key.equals("Tab")) {
                    proc.getOutputStream().write("\t".getBytes());
                }
                proc.getOutputStream().flush();
            } catch (Exception ex) {
                LOGGER.error("Key", ex);
            }
        });

        var xTermMenuBar = new MenuBar();
        menuItemEsc = xTermMenuBar.addItem("ESC", e -> {
            MLang.tryThis(() -> proc.getOutputStream().write("\u001b".getBytes()));
            xterm.focus();
        });
        menuItemEsc = xTermMenuBar.addItem("TAB", e -> {
            MLang.tryThis(() -> proc.getOutputStream().write("\t".getBytes()));
            xterm.focus();
        });
        menuItemEsc = xTermMenuBar.addItem("Ctrl+C", e -> {
            MLang.tryThis(() -> proc.getOutputStream().write("\u0003".getBytes()));
            xterm.focus();
        });

        add(xTermMenuBar);
        add(xterm);

        xterm.focus();

        setSizeFull();
        setPadding(false);
        setMargin(false);

        try {
            ProcessBuilder builder = new ProcessBuilder("/bin/bash");
            builder.redirectErrorStream(true);
            proc = builder.start();
            Thread.startVirtualThread(this::readFromTerminalLoop);
        } catch (Exception e) {
            LOGGER.error("Error", e);
            UiUtil.showErrorNotification("Error creating bash " + e.getMessage());
        }

    }

    private void readFromTerminalLoop() {
        try {
            byte[] buffer = new byte[1024];
            InputStream is = proc.getInputStream();
            while (true) {
                int len = is.read(buffer);
                if (len <= 0) {
                    MThread.sleep(100);
                    continue;
                }
                System.out.println("SH Read: " + len);
                String line = new String(buffer, 0, len);
                core.ui().access(() -> xterm.write(line));
            }
        } catch (Exception e) {
            LOGGER.error("Loop", e);
        }
    }

    private void closeTerminal() {
        proc.destroy();
        proc = null;
    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabUnselected() {

    }

    @Override
    public void tabDestroyed() {
        closeTerminal();
    }

    @Override
    public void tabRefresh(long counter) {

    }

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }
}
