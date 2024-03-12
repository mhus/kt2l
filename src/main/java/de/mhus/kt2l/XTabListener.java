package de.mhus.kt2l;

import com.vaadin.flow.component.ShortcutEvent;

public interface XTabListener {
    void tabInit(XTab xTab);
    void tabSelected();
    void tabDeselected();
    void tabDestroyed();

    void tabRefresh();

    void tabShortcut(ShortcutEvent event);
}
