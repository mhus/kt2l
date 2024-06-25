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
package de.mhus.kt2l.resources.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tree.MProperties;
import de.mhus.commons.yaml.MYaml;
import de.mhus.commons.yaml.YElement;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.help.HelpResourceConnector;
import de.mhus.kt2l.k8s.HandlerK8s;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.ui.ProgressDialog;
import de.mhus.kt2l.ui.UiUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.mhus.commons.tools.MString.isEmpty;
import static de.mhus.commons.tools.MString.isSet;

@Configurable
@Slf4j
public class ResourceCreatePanel extends VerticalLayout implements DeskTabListener, HelpResourceConnector {
    private final Cluster cluster;
    private final Core core;
    private final String namespace;
    private DeskTab tab;
    private AceEditor editor;

    @Autowired
    private K8sService k8s;
    private MenuItem tmplEnablesToggle;

    public ResourceCreatePanel(Cluster cluster, Core core, String namespace) {
        this.cluster = cluster;
        this.core = core;
        this.namespace = namespace;
    }

    @Override
    public void tabInit(DeskTab deskTab) {
        this.tab = deskTab;

        var menuBar = new MenuBar();
        var tmplItem = menuBar.addItem("Templates");
        var tmplSub = tmplItem.getSubMenu();
        tmplSub.addItem("Open Form", e -> {
            fillTemplate();
        });
        tmplEnablesToggle = tmplSub.addItem("Enabled");
        tmplEnablesToggle.setCheckable(true);
        tmplEnablesToggle.setChecked(true);

        var createItem = menuBar.addItem(VaadinIcon.FORWARD.create(), e -> {
            createResource();
        });
        createItem.add("Create");

        var deleteItem = menuBar.addItem(VaadinIcon.FILE_REMOVE.create(), e -> {
            ConfirmDialog confirm = new ConfirmDialog();
            confirm.setText("Delete resources?");
            confirm.setCloseOnEsc(true);
            confirm.setCancelable(true);
            confirm.setConfirmText("Delete");
            confirm.setCancelText("Cancel");
            confirm.addConfirmListener(this::deleteResource);
            confirm.open();
        });
        deleteItem.add("Delete");

        add(menuBar);


        editor = new AceEditor();
        editor.setTheme(AceTheme.terminal);
        editor.setMode(AceMode.yaml);
//        resYamlEditor.setValue("");
        editor.setReadOnly(false);
        editor.setSizeFull();

        add(editor);
        setSizeFull();
        setPadding(false);
        setMargin(false);

    }

    private void deleteResource(ConfirmDialog.ConfirmEvent confirmEvent) {

        final var parts = splitContent();
        parseContent(parts);
        substituteContent(parts);

        ProgressDialog dialog = new ProgressDialog();
        dialog.setHeaderTitle("Delete");
        dialog.setMax(parts.size());
        dialog.open();

        Thread.startVirtualThread(() -> {
            for (ContentEntry entry : parts.reversed()) {
                core.ui().access(() -> {
                    try {
                        dialog.setProgress(dialog.getProgress() + 1, entry.kind);
                        var metadata = entry.yaml.asMap().getMap("metadata");
                        var resName = metadata.getString("name");
                        var resNamespace = metadata.getString("namespace");
                        entry.handler.delete(cluster.getApiProvider(), resName, resNamespace);
                        UiUtil.showSuccessNotification("Resource deleted: " + entry.kind);
                    } catch (Exception t) {
                        LOGGER.error("Error creating resource", t);
                        UiUtil.showErrorNotification("Error deleting resource", t);
                    }
                });
            }
            dialog.close();
        });
    }

    private void createResource() {
        final var parts = splitContent();
        if (!parseContent(parts)) return;
        if (!substituteContent(parts)) return;

        ProgressDialog dialog = new ProgressDialog();
        dialog.setHeaderTitle("Create");
        dialog.setMax(parts.size());
        dialog.open();

        Thread.startVirtualThread(() -> {
            AtomicBoolean exit = new AtomicBoolean(false);
            for (ContentEntry entry : parts) {
                if (exit.get()) break;
                core.ui().access(() -> {
                    try {
                        dialog.setProgress(dialog.getProgress() + 1, entry.kind);
                        entry.handler.create(cluster.getApiProvider(), entry.preparedContent);
                        UiUtil.showSuccessNotification("Resource created: " + entry.kind);
                    } catch (Exception t) {
                        LOGGER.error("Error creating resource", t);
                        UiUtil.showErrorNotification("Error creating resource", t);
                        dialog.close();
                        exit.set(true);
                    }
                });
            }
            dialog.close();
        });
    }

    private boolean substituteContent(List<ContentEntry> parts) {
        try {
            if (tmplEnablesToggle.isChecked()) {
                for (ContentEntry entry : parts) {
                    MProperties properties = new MProperties();
                    properties.setString("namespace", namespace);
                    for (TemplateEntry template : entry.templates) {
                        if (!template.valid) continue;
                        properties.setString(template.name, template.value);
                    }
                    entry.preparedContent = MString.substitute(entry.content, properties);
                    entry.handler = k8s.getTypeHandler(K8sUtil.toType(entry.kind));
                    if (entry.handler == null) {
                        UiUtil.showErrorNotification("Resource not supported: " + entry.kind);
                        return false;
                    }
                }
            } else {
                for (ContentEntry entry : parts) {
                    entry.preparedContent = entry.content;
                }
            }
        } catch (Exception t) {
            LOGGER.error("Error preparing content", t);
            UiUtil.showErrorNotification("Error preparing content", t);
            return false;
        }
        return true;
    }

    private boolean parseContent(List<ContentEntry> parts) {
        for (ContentEntry entry : parts) {
            try {
                entry.parseYaml();
                entry.parseTemplate();
            } catch (Exception t) {
                LOGGER.error("Error parsing content", t);
                UiUtil.showErrorNotification("Error parsing content", t);
                return false;
            }
        }
        return true;
    }

    private void fillTemplate() {
        var layout = new VerticalLayout();
        final var parts = splitContent();
        for (ContentEntry entry : parts) {
            try {
                try {
                    entry.parseYaml();
                } catch (Exception t) {}
                entry.parseTemplate();

                var title = new Div(entry.kind);
                layout.add(title);
                for (TemplateEntry template : entry.templates) {
                    if (!template.valid) continue;
                    switch (template.type) {
                        case STRING:
                            var text = new TextField(template.description);
                            text.setLabel(template.name);
                            if (template.value != null)
                                text.setValue(template.value);
                            layout.add(text);
                            template.component = text;
                            break;
                        case BOOLEAN:
                            var bool = new Checkbox(template.description);
                            bool.setLabel(template.name);
                            if (template.value != null)
                                bool.setValue(template.value.equalsIgnoreCase("true"));
                            layout.add(bool);
                            template.component = bool;
                            break;
                        case INTEGER:
                            var integer = new TextField(template.description);
                            integer.setLabel(template.name);
                            if (template.value != null)
                                integer.setValue(template.value);
                            layout.add(integer);
                            template.component = integer;
                            break;
                        case OPTION:
                            var options = new ComboBox<String>();
                            options.setLabel(template.name);
                            options.setItems(template.options.split(","));
                            if (template.value != null)
                                options.setValue(template.value);
                            layout.add(options);
                            template.component = options;
                            break;
                    }
                }


            } catch (Throwable t) {
                LOGGER.error("Error parsing content", t);
            }
        }

        layout.setSizeFull();
        Dialog dialog = new Dialog();
        dialog.add(layout);
        dialog.setWidth("400px");
        dialog.setHeight("80%");

        dialog.setHeaderTitle("Fill Template");
        dialog.getFooter().add(
                new Button("Cancel", e -> dialog.close())
        );
        dialog.getFooter().add(
                new Button("Ok", e -> {
                    fillTemplatePart2(parts);
                    dialog.close();
                })
        );

        dialog.open();

    }

    private void fillTemplatePart2(List<ContentEntry> parts) {
        StringBuilder out = new StringBuilder();
        for (ContentEntry entry : parts) {
            if (out.length() > 0)
                out.append("\n---\n");

            if (entry.beforeTemplate != null)
                out.append(entry.beforeTemplate);
            if (entry.templates.size() > 0) {
                out.append("# TEMPLATE BEGIN\n");
                try {
                    for (TemplateEntry template : entry.templates) {
                        if (!template.valid) continue;
                        var value = template.component;
                        if (value instanceof TextField) {
                            template.value = ((TextField)value).getValue();
                        } else if (value instanceof Checkbox) {
                            template.value = ((Checkbox)value).getValue() ? "true" : "false";
                        } else if (value instanceof ComboBox) {
                            template.value = ((ComboBox<String>)value).getValue();
                        }
                        out.append("# ").append(template.toString()).append("\n");
                    }
                } catch (Throwable t) {
                    LOGGER.error("Error parsing content", t);
                }
                out.append("# TEMPLATE END\n");
            }
            if (entry.afterTemplate != null)
                out.append(entry.afterTemplate);

        }

        editor.setValue(out.toString());
    }

    private List<ContentEntry> splitContent() {
        return Arrays.stream(editor.getValue().split("\n---(w*)\n")).map((e) -> new ContentEntry(e)).toList();
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
    public String getHelpContent() {
        return editor.getValue();
    }

    @Override
    public void setHelpContent(String content) {
        editor.setValue(content);
    }

    @Override
    public int getHelpCursorPos() {
        return editor.getCursorPosition().getIndex();
    }

    private class ContentEntry {

        private final String content;
        public String preparedContent;
        public HandlerK8s handler;
        private List<TemplateEntry> templates = new ArrayList<>();
        private YElement yaml;
        private String kind;
        private String beforeTemplate;
        private String afterTemplate;

        public ContentEntry(String content) {
            this.content = content;
        }

        public void parseYaml() {
            yaml = MYaml.loadFromString(content);
            kind = yaml.asMap().getString("kind").toString();
        }

        public void parseTemplate() {
            var templateMode = false;
            templates.clear();
            StringBuilder stringBuilder = new StringBuilder();
            for (var line : content.split("\n")) {
                if (beforeTemplate != null && !templateMode) {
                    stringBuilder.append(line).append("\n");
                } else
                if (line.startsWith("#")) {
                    var trim = line.substring(1).trim();
                    if (trim.equals("TEMPLATE BEGIN")) {
                        beforeTemplate = stringBuilder.toString();
                        stringBuilder = new StringBuilder();
                        templateMode = true;
                    } else if (trim.equals("TEMPLATE END")) {
                        templateMode = false;
                    } else if (templateMode) {
                        templates.add(new TemplateEntry(trim));
                    } else {
                        stringBuilder.append(line).append("\n");
                    }
                } else {
                    stringBuilder.append(line).append("\n");
                }
            }
            afterTemplate = stringBuilder.toString();
        }

    }

    private class TemplateEntry {
        public Component component;
        enum TYPE {STRING,BOOLEAN,INTEGER,OPTION}
        private final String content;
        private String name;
        private TYPE type;
        private String options;
        private String description;
        private String value;
        private boolean valid = true;

        public TemplateEntry(String content) {
            this.content = content;
            parse();
            validate();
        }

        public String toString() {
            if (valid)
                return  name + " : " +
                        type +
                        (isSet(options) ? " (" + options + ")" : "") +
                        (isSet(description) ? " <" + description + ">" : "") +
                        ": " + value;
            else
                return content;
        }


        private void validate() {
            if (!valid) return;
            if (isEmpty(name)) {
                LOGGER.info("Missing name in template: " + content);
                valid = false;
            }
            if (type == null) {
                LOGGER.info("Missing type in template: " + content);
                valid = false;
            }
            if (value != null) {
                switch (type) {
                    case BOOLEAN:
                        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                            LOGGER.info("Invalid value for boolean in template: " + content);
                            valid = false;
                        }
                        break;
                    case INTEGER:
                        try {
                            Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            LOGGER.info("Invalid value for integer in template: " + content);
                            valid = false;
                        }
                        break;
                    case OPTION:
                        if (isEmpty(options)) {
                            LOGGER.info("Missing options in template: " + content);
                            valid = false;
                        } else {
                            var found = false;
                            for (var opt : options.split(",")) {
                                if (opt.trim().equalsIgnoreCase(value)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                LOGGER.info("Invalid value for option in template: " + content);
                                valid = false;
                            }
                        }
                        break;
                }
            }
        }

        // name : type (options) <description>: value
        private void parse() {
            var c = content;
            var pos = c.indexOf(":");
            if (pos < 0) {
                name = c;
                return;
            }
            name = c.substring(0, pos).trim();
            c = c.substring(pos + 1);
            pos = c.indexOf("(");
            if (pos >= 0) {
                var pos2 = c.indexOf(")");
                if (pos2 < 0) {
                    LOGGER.info("Missing ) in template: " + content);
                    valid = false;
                    return;
                }
                options = c.substring(pos + 1, pos2).trim();
                c = c.substring(0, pos) + c.substring(pos2 + 1);
            }
            pos = c.indexOf("<");
            if (pos >= 0) {
                var pos2 = c.indexOf(">");
                if (pos2 < 0) {
                    LOGGER.info("Missing > in template: " + content);
                    valid = false;
                    return;
                }
                description = c.substring(pos + 1, pos2).trim();
                c = c.substring(0, pos) + c.substring(pos2 + 1);
            }
            pos = c.indexOf(":");
            if (pos >= 0) {
                value = c.substring(pos + 1).trim();
                c = c.substring(0, pos).trim();
            }
            final var cFinal = c.trim().toUpperCase();
            type = Arrays.stream(TYPE.values()).filter(e -> e.name().equals(cFinal)).findFirst().orElse(null);
            if (type == null) {
                LOGGER.info("Unknown type in template: " + content);
                valid = false;
            }
        }
    }
}
