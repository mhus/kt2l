/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l.form;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.mhus.commons.tools.MFile;
import de.mhus.commons.tree.ITreeNode;
import de.mhus.commons.tree.MTree;
import de.mhus.commons.tree.TreeNode;
import de.mhus.commons.tree.TreeNodeList;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class YArray extends YComponent<String> {

    private VerticalLayout main;
    private Consumer<FormPanelVerticalLayout> createConsumer;
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
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidthFull();
        if (!readOnly) {
            buttons.add(new Button(VaadinIcon.PLUS.create(), e -> insertBlockAfterAll(new TreeNode())));
        }
        buttons.add(new Div(label));
        main.add(buttons);
        getParent(content).getArray(getNodeName()).orElse(MTree.EMPTY_LIST).forEach(node -> insertBlockAfterAll(node));
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
        if (!readOnly) {
            buttons.add(new Button(VaadinIcon.PLUS.create(), e -> insertBlockBefore(panel, node)));
            buttons.add(new Button(VaadinIcon.MINUS.create(), e -> removeBlock(panel, node)));
        }
        buttons.add(new Div(label));
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
        if (!readOnly) {
            buttons.add(new Button(VaadinIcon.PLUS.create(), e -> insertBlockBefore(panel, node)));
            buttons.add(new Button(VaadinIcon.MINUS.create(), e -> removeBlock(panel, node)));
        }
        buttons.add(new Div(label));
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
        TreeNodeList content = new TreeNodeList(path, (TreeNode)node);
        panels.forEach(ps -> {
            var child = ps.node();
            ps.panel.save(child);
            content.add(child);
        });
        node.getObjectByPath(MTree.getParentPath(path)).orElse(node).put(MTree.getNodeName(path), content);
    }

    public YArray create(Consumer<FormPanelVerticalLayout> consumer) {
        createConsumer = consumer;
        return this;
    }

    private class XPanel extends FormPanelVerticalLayout {
        public XPanel(Consumer<FormPanelVerticalLayout> createConsumer) {
            super();
        }

        @Override
        public void initUi() {
            createConsumer.accept(this);
        }
    }

    private record PanelStore(XPanel panel, ITreeNode node, HorizontalLayout buttons, Hr hr) {
    }
}
