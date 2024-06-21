/*
 * kt2l-desktop - kt2l desktop app
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
package de.mhus.kt2l;

import de.mhus.commons.tools.MArgs;
import de.mhus.commons.tools.MFile;
import de.mhus.commons.tools.MThread;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.VisibilityWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.io.InputStream;

@Slf4j
public class BrowserInstance {

    public static MArgs.Option browserType;
    private Shell shell;
    private Browser browser;
    private int tabIndex;
    private final Display display;

    public BrowserInstance() {
        display = Kt2lDesktopApplication.getDisplay();
        // display.asyncExec(this::init);
        init();
    }

    public void init() {

        shell = new Shell(display);
        Kt2lDesktopApplication.register(this);

        try {
            ClassLoader loader = Kt2lDesktopApplication.class.getClassLoader();
            InputStream is128 = loader.getResourceAsStream(
                    "icons/kt2l128.png");
            Image icon128 = new Image(display, is128);
            is128.close();

//            shell.setImages(new Image[]{icon16, icon32, icon48, icon128});
            shell.setImages(new Image[]{icon128});
        } catch (Exception e) {
            LOGGER.warn("Icons not found", e);
        }

        shell.addListener(SWT.Close, event -> {
            Kt2lDesktopApplication.unregister(this);
            event.doit = false;
            shell.dispose();

//                int style = SWT.APPLICATION_MODAL | SWT.YES | SWT.NO;
//                MessageBox messageBox = new MessageBox (shell, style);
//                messageBox.setText ("Information");
//                messageBox.setMessage ("Close the shell?");
//                event.doit = messageBox.open () == SWT.YES;

        });

        final Rectangle screenSize = display.getPrimaryMonitor().getBounds();
        shell.setSize(screenSize.width, screenSize.height);
        shell.setLocation(0, 0);
        shell.setLayout(new FillLayout());

        CTabFolder tabFolder = new CTabFolder(shell, SWT.TOP );
        browser = addNewBrowser(tabFolder, " KT2L ", false);

        shell.open();


    }

    public Display getDisplay() {
        return display;
    }

    public Shell getShell() {
        return shell;
    }

    public Browser getBrowser() {
        return browser;
    }

    private Browser addNewBrowser(CTabFolder folder, String title, boolean closeable)
    {
        CTabItem item = new CTabItem(folder, SWT.NONE | (closeable ? SWT.CLOSE : 0));
        Composite c = new Composite(folder, SWT.NONE);
        item.setControl(c);
        c.setLayout(new FillLayout());

        Browser browser = new Browser(c, switch(browserType.getValue("none").toLowerCase()) {
            case "mozilla" -> SWT.MOZILLA;
            case "webkit" -> SWT.WEBKIT;
            case "chromium" -> SWT.CHROMIUM;
            default -> SWT.NONE;
        });

        item.setText(title);

        browser.addOpenWindowListener(e ->
        {
            e.browser = addNewBrowser(folder, " Tab " + (++tabIndex) + " ", true);
        });
        browser.addVisibilityWindowListener(new VisibilityWindowListener()
        {
            @Override
            public void hide(WindowEvent e)
            {
                Browser browser = (Browser) e.widget;
                Shell shell = browser.getShell();
                shell.setVisible(false);
            }

            @Override
            public void show(WindowEvent e)
            {
                Browser browser = (Browser) e.widget;
                final Shell shell = browser.getShell();
                if (e.location != null) shell.setLocation(e.location);
                if (e.size != null)
                {
                    Point size = e.size;
                    shell.setSize(shell.computeSize(size.x, size.y));
                }
                shell.open();
            }
        });
        browser.addCloseWindowListener(e ->
        {
            Browser browser1 = (Browser) e.widget;
            Shell shell = browser1.getShell();
            shell.close();
        });

        folder.setSelection(item);

        return browser;
    }

    public static void setStartupMessage() {
        var html = new StringBuffer();
        html.append("<html><body>Booting [KT2L] ...<br><br><br><center>");
        try {
            html.append( MFile.readFile(Kt2lApplication.class.getResourceAsStream("/images/kt2l-logo.svg") ) );
        } catch (Exception e) {
            LOGGER.warn("Logo not found", e);
        }
        html.append("</center></body></html>");
        var htmlStr = html.toString();
        Kt2lDesktopApplication.getBrowserInstances().forEach(b -> b.getBrowser().setText(htmlStr));
    }

}
