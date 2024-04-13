package de.mhus.kt2l.core;

import com.vaadin.flow.component.ShortcutEvent;

public interface XTabListener {
    void tabInit(XTab xTab);
    void tabSelected();
    void tabUnselected();
    void tabDestroyed();

    void tabRefresh(long counter);

    void tabShortcut(ShortcutEvent event);
}
