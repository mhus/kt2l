package de.mhus.kt2l;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class XTabViewer extends VerticalLayout {

    private final MainView mainView;
    private List<XTab> tabs = new LinkedList<>();

    public XTabViewer(MainView mainView) {
        setWidthFull();
        this.mainView = mainView;
    }

    public XTab addTab(String id, String title, boolean closeable, boolean unique, Icon icon, Supplier<Component> panelCreator, Object ... parameters) {
        if (unique) {
            Optional<XTab> tab = getTab(id);
            if (tab.isPresent()) {
                return tab.get();
            }
        }
        final var newTab = new XTab(id, title, closeable, icon, panelCreator.get());
        if (parameters != null && parameters.length > 0) {
            if (parameters.length % 2 != 0)
                throw new IllegalArgumentException("Parameters must be in pairs");
            for (int i = 0; i < parameters.length; i += 2) {
                newTab.getParameters().put(parameters[i].toString(), parameters[i + 1]);
            }
        }
        return addTab(newTab);
    }

    public XTab addTab(XTab tab) {
        tabs.add(tab);
        tab.setXTabViewer(this);
        add(tab);
        return tab;
    }

    public void closeTab(XTab tab) {
        tabs.remove(tab);
        remove(tab);
    }

    public Optional<XTab> getTab(String main) {
        return tabs.stream().filter(t -> t.getTabId().equals(main)).findFirst();
    }

    public void setPanel(Component panel) {
        mainView.setContent(panel);
//        mainView.showRouterLayoutContent(panel);
    }

    public MainView getMainView() {
        return mainView;
    }
}
