package de.mhus.kt2l;

import de.mhus.commons.tools.MSystem;
import de.mhus.commons.tools.MThread;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.CloseWindowListener;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.VisibilityWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.InputStream;

@Slf4j
public class Kt2lDesktopApplication extends Kt2lApplication {

    private static Display display;
    private static Shell shell;
    private static Browser browser;
    private static int tabIndex;

    public static void main(String[] args) {
        Display.setAppName("KT2L");
        display = new Display();
        shell = new Shell(display);

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
            BrowserBean.setShutdownMessage();
            Thread.startVirtualThread(() -> {
                MThread.sleep(1000);
                System.exit(0);
            });
            event.doit = false;

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
        BrowserBean.setStartupMessage();

//        browser.addLocationListener(new LocationListener() {
//            @Override
//            public void changing(LocationEvent event) {
//                LOGGER.debug("Browser changing {}", event);
//                if (event.location.startsWith("http") && !event.location.startsWith("http://localhost")) {
//                    MSystem.openBrowserUrl(event.location);
//                    event.doit = false;
//                }
//            }
//
//            @Override
//            public void changed(LocationEvent event) {
//                //LOGGER.debug("Browser changed {} on top {}", event.location,event.top);
//            }
//        });


        shell.open();

        Thread.startVirtualThread(() -> {
            SpringApplicationBuilder builder = new SpringApplicationBuilder(Kt2lApplication.class);
            builder.headless(false);
            ConfigurableApplicationContext context = builder.run(args);
        });

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        display.dispose();

    }

    public static Display getDisplay() {
        return display;
    }

    public static Shell getShell() {
        return shell;
    }

    public static Browser getBrowser() {
        return browser;
    }

    private static Browser addNewBrowser(CTabFolder folder, String title, boolean closeable)
    {
       CTabItem item = new CTabItem(folder, SWT.NONE | (closeable ? SWT.CLOSE : 0));
        Composite c = new Composite(folder, SWT.NONE);
        item.setControl(c);
        c.setLayout(new FillLayout());

        Browser browser = new Browser(c, SWT.NONE);

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
}
