package de.mhus.kt2l;

public interface XTabListener {
    void tabInit(XTab xTab);
    void tabSelected();
    void tabClosed();
    void tabDestroyed();

    void tabRefresh();
}
