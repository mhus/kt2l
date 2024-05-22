package de.mhus.kt2l.cfg.panel;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.commons.tree.TreeNode;
import de.mhus.commons.tree.TreeNodeList;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class YArray extends YComponent<String> {

    private VerticalLayout main;
    private Consumer<CPanelVerticalLayout> createConsumer;
    private final List<PanelStore> panels = new LinkedList<>();


    @Override
    public void initUi() {
        main = new VerticalLayout();
        main.setSizeFull();
    }

    @Override
    public Component getComponent() {
        return main;
    }

    @Override
    public void load(ITreeNode content) {
        main.removeAll();
        panels.clear();

        main.add(new Hr());
        main.add(new Button(VaadinIcon.PLUS.create(), e -> insertBlockAfterAll(new TreeNode())));

        content.getArray(name).orElse(MTree.EMPTY_LIST).forEach(node -> insertBlockAfterAll(node));


    }

    private void removeBlock(XPanel panel, ITreeNode node) {
        var ps = panels.stream().filter(p -> p.panel() == panel).findFirst().orElse(null);
        main.remove(panel);
        main.remove(ps.buttons());
        main.remove(ps.hr());
        panels.removeIf(p -> p.panel() == panel);
    }

    private void insertBlockBefore(XPanel beforePanel, ITreeNode beforeNode) {
        var panel = new XPanel(createConsumer);

        ITreeNode node = new TreeNode();
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidthFull();
        buttons.add(new Button(VaadinIcon.PLUS.create(), e -> insertBlockBefore( panel, node)));
        buttons.add(new Button(VaadinIcon.MINUS.create(), e -> removeBlock( panel, node)));
        var hr = new Hr();

        panel.initUi();
        int index = 0;
        for (int i = 0; i < main.getComponentCount(); i++)
            if (main.getComponentAt(i) == beforePanel) {
                index = i;
                break;
            }
        main.addComponentAtIndex(index-2, panel);
        main.addComponentAtIndex(index-2, buttons);
        main.addComponentAtIndex(index-2, hr);
        panel.load(node);
        panels.add(new PanelStore(panel, node, buttons, hr)); // order not needed
    }

    private void insertBlockAfterAll(ITreeNode node) {
        var panel = new XPanel(createConsumer);

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidthFull();
        buttons.add(new Button(VaadinIcon.PLUS.create(), e -> insertBlockBefore( panel, node)));
        buttons.add(new Button(VaadinIcon.MINUS.create(), e -> removeBlock( panel, node)));
        var hr = new Hr();

        panel.initUi();
        main.addComponentAtIndex(main.getComponentCount()-2, hr); // last one is the 'add' button
        main.addComponentAtIndex(main.getComponentCount()-2, buttons); // last one is the 'add' button
        main.addComponentAtIndex(main.getComponentCount()-2, panel); // last one is the 'add' button
        panel.load(node);
        panels.add(new PanelStore(panel, node, buttons, hr));

    }

    @Override
    public void save(ITreeNode node) {
        TreeNodeList content = new TreeNodeList(name, (TreeNode)node);
        panels.forEach(ps -> {
            var child = ps.node();
            ps.panel.save(child);
            content.add(child);
        });
        node.put(name, content);
    }

    public YArray create(Consumer<CPanelVerticalLayout> consumer) {
        createConsumer = consumer;
        return this;
    }

    private class XPanel extends CPanelVerticalLayout {
        public XPanel(Consumer<CPanelVerticalLayout> createConsumer) {
            super();
        }

        @Override
        public String getTitle() {
            return null;
        }

        @Override
        public void initUi() {
            createConsumer.accept(this);
        }
    }

    private record PanelStore(XPanel panel, ITreeNode node, HorizontalLayout buttons, Hr hr) {
    }
}
