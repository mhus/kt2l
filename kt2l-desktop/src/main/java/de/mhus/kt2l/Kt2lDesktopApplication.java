package de.mhus.kt2l;

import de.mhus.commons.tools.MFile;
import de.mhus.commons.tools.MLang;
import de.mhus.commons.tools.MThread;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class Kt2lDesktopApplication extends Kt2lApplication {

    private static Display display;
    private static Shell shell;
    private static Browser browser;

    public static void main(String[] args) {
        Display.setAppName("KT2L");
        display = new Display();
        shell = new Shell(display);

        try {
            ClassLoader loader = Kt2lDesktopApplication.class.getClassLoader();
//            InputStream is16 = loader.getResourceAsStream(
//                    "icons/kt2l16.gif");
//            Image icon16 = new Image(display, is16);
//            is16.close();
//            InputStream is32 = loader.getResourceAsStream(
//                    "icons/kt2l32.gif");
//            Image icon32 = new Image(display, is32);
//            is32.close();
//
//            InputStream is48 = loader.getResourceAsStream(
//                    "icons/kt2l48.gif");
//            Image icon48 = new Image(display, is48);
//            is48.close();

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


        Rectangle screenSize = display.getPrimaryMonitor().getBounds();
        shell.setSize(screenSize.width, screenSize.height);
        shell.setLocation(0, 0);
//        shell.setLocation((screenSize.width - shell.getBounds().width) / 2, (screenSize.height - shell.getBounds().height) / 2);

        browser = new Browser(shell, SWT.NONE);
        BrowserBean.setStartupMessage();
        browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        shell.setLayout(new GridLayout());

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

}
