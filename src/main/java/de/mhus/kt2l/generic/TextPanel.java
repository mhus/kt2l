package de.mhus.kt2l.generic;

import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import de.mhus.kt2l.ui.XTab;
import de.mhus.kt2l.ui.XTabListener;

public class TextPanel extends VerticalLayout implements XTabListener {

    private final String content;
    private TextArea text;

    public TextPanel(String content) {
        this.content = content;
    }

    @Override
    public void tabInit(XTab xTab) {
        text = new TextArea();
        text.setSizeFull();
        text.setReadOnly(true);
        add(text);
        text.setValue(content);
        setSizeFull();
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
    public void tabRefresh(long counter) {

    }

    @Override
    public void tabShortcut(ShortcutEvent event) {

    }
}
