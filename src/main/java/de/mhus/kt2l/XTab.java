package de.mhus.kt2l;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class XTab extends HorizontalLayout {

    private final Icon icon;
    @Getter
    private final String tabId;
    private final Text text;
    @Getter
    private final boolean closeable;
    @Getter
    private final Component panel;
    @Getter
    private XTabViewer viewer;
    @Getter
    private Map<String, Object> parameters = new HashMap<>();

    public XTab(String tabId, String title, boolean closeable, Icon icon, Component panel) {
        if (icon == null)
            icon = VaadinIcon.FILE.create();
        this.icon = icon;
        this.tabId = tabId;
        this.closeable = closeable;
        this.panel = panel;
        text = new Text(title);
        add(icon, text);
        if (closeable) {
            Icon close = VaadinIcon.CLOSE.create();
            close.addClickListener(click -> {
                closeTab();
            });
            add(close);
        }

        setWidthFull();

    }

    public void closeTab() {
        if (this.panel != null && this.panel instanceof XTabListener)
            ((XTabListener) this.panel).tabDestroyed();
    }

    public void setXTabViewer(XTabViewer tabViewer) {
        this.viewer = tabViewer;
        viewer.getMainView().getBeanFactory().autowireBean(panel);
        if (panel instanceof XTabListener) {
            ((XTabListener) panel).tabInit(this);
        }
    }

    public void select() {
        if (panel != null) {
            if (panel instanceof XTabListener)
                ((XTabListener) panel).tabSelected();
            viewer.setPanel(panel);
        }
    }
}
