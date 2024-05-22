package de.mhus.kt2l.cfg.panel;

import com.vaadin.flow.component.Component;
import de.mhus.commons.tree.ITreeNode;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public abstract class YComponent<T> {

    @Setter
    protected String name;
    @Setter
    protected String label;
    @Setter
    protected T defaultValue;

    public abstract void initUi();

    public abstract Component getComponent();

    public abstract void load(ITreeNode content);

    public abstract void save(ITreeNode node);

}
