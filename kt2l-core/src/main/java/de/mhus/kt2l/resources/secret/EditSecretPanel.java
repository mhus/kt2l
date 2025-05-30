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
package de.mhus.kt2l.resources.secret;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoIcon;
import de.mhus.commons.lang.IRegistration;
import de.mhus.kt2l.cluster.Cluster;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.DeskTab;
import de.mhus.kt2l.core.DeskTabCloseClickedListener;
import de.mhus.kt2l.core.DeskTabListener;
import de.mhus.kt2l.k8s.K8s;
import de.mhus.kt2l.k8s.K8sService;
import de.mhus.kt2l.k8s.K8sUtil;
import de.mhus.kt2l.ui.UiUtil;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.util.Watch;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.concurrent.atomic.AtomicInteger;

@Configurable
@Slf4j
public class EditSecretPanel extends VerticalLayout implements DeskTabListener, DeskTabCloseClickedListener {

    @Autowired
    private K8sService k8sService;

    private final Core core;
    private final Cluster cluster;
    private V1Secret selected;
    private IRegistration registration;
    private Div status;
    private VerticalLayout entryList;
    private boolean keyEditMode;
    private MenuItem addMenuItem;
    private MenuItem saveMenuItem;
    private MenuItem reloadMenuItem;

    public EditSecretPanel(Core core, Cluster cluster, V1Secret selected) {
        this.core = core;
        this.cluster = cluster;
        this.selected = selected;
    }

    @Override
    public void tabInit(DeskTab deskTab) {
        registration = core.backgroundJobInstance(cluster, SecretWatch.class).getEventHandler().registerWeak(this::changedEvent);

        status = new Div();
        status.setWidthFull();
        status.addClassName("color-grey");
        status.setText("Secret created on server " + selected.getMetadata().getCreationTimestamp());
        add(status);

        var menuBar = new MenuBar();

        addMenuItem = menuBar.addItem(LumoIcon.PLUS.create(), e -> addNewEntry());
        addMenuItem.getElement().setAttribute("title", "Add new entry");

        saveMenuItem = menuBar.addItem("Save", e -> doSave());
        menuBar.setWidthFull();
        add(menuBar);

        reloadMenuItem = menuBar.addItem("Reload", e -> doReload());
        menuBar.setWidthFull();
        add(menuBar);

        entryList = new VerticalLayout();
        entryList.setSizeFull();
        entryList.setMargin(false);
        entryList.setPadding(false);
        add(entryList);
        updateEntryList();

        setSizeFull();
        setMargin(false);
        setPadding(false);
    }

    private void doReload() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Reload entry");
        dialog.setText("Do you really want to reload Secret from server?");
        dialog.setCloseOnEsc(true);
        dialog.setCancelable(true);
        dialog.addConfirmListener(e2 -> {
            try {
                var cm = cluster.getApiProvider().getCoreV1Api().readNamespacedSecret(selected.getMetadata().getName(), selected.getMetadata().getNamespace()).execute();
                selected = cm;
                entryList.removeAll();
                updateEntryList();
            } catch (Exception e) {
                UiUtil.showErrorNotification("Error reloading Secret", e);
            }
        });
        dialog.open();
    }

    private void doSave() {
        var data = selected.getData();
        data.clear();
        for (int i = 0; i < entryList.getComponentCount(); i++) {
            Entry entry = (Entry)entryList.getComponentAt(i);
            data.put(entry.key, stringToSecret(entry.getValue()));
        }

        try {
            var yaml = Yaml.dump(selected);
            k8sService.getTypeHandler(K8s.SECRET).replace(cluster.getApiProvider(), selected.getMetadata().getName(), selected.getMetadata().getNamespace(), yaml);
        } catch (Exception e) {
            UiUtil.showErrorNotification("Error saving Secret", e);
            return;
        }

        for (int i = 0; i < entryList.getComponentCount(); i++) {
            Entry entry = (Entry) entryList.getComponentAt(i);
            entry.setNewEntry(false);
            entry.setChanged(false);
        }

        UiUtil.showSuccessNotification("Secret saved");
    }

    private void addNewEntry() {
        var entry = new Entry("", "");
        entryList.addComponentAtIndex(0, entry);
        entry.setNewEntry(true);
        setKeyEditMode(true);
        entry.keyField.setReadOnly(false);
        entry.keyField.focus();
    }

    private void setKeyEditMode(boolean mode) {
        if (mode == keyEditMode) return;

        addMenuItem.setEnabled(!mode);
        saveMenuItem.setEnabled(!mode);
        reloadMenuItem.setEnabled(!mode);

        for (int i = 0; i < entryList.getComponentCount(); i++) {
            Entry entry = (Entry)entryList.getComponentAt(i);
            entry.setEntryKeyEditMode(mode);
        }
        this.keyEditMode = mode;
    }

    private synchronized void updateEntryList() {
        var data = selected.getData();
        if (data == null) {
            entryList.removeAll();
            return;
        }

        var keyList = data.keySet().stream().sorted().toList();
        final AtomicInteger index = new AtomicInteger(-1);
        keyList.forEach(key -> {
            index.incrementAndGet();
            var value = data.get(key);
            for (int i = 0; i < entryList.getComponentCount(); i++) {
                Entry entry = (Entry)entryList.getComponentAt(i);
                if (entry.key.equals(key)) {
                    LOGGER.debug("Update entry: {}", key);
                    entry.updateValue(secretToString(value));
                    return;
                }
            }
            LOGGER.debug("Add entry: {}", key);
            var entry = new Entry(key, secretToString(value));
            entryList.addComponentAtIndex(index.get(), entry);
        });

    }

    private String secretToString(byte[] value) {
        try {
            return new String(value);
        } catch (Exception e) {
            LOGGER.warn("Can't decode secret", e);
            return "ERROR";
        }
    }

    private byte[] stringToSecret(String value) {
        return value.getBytes();
    }

    private void changedEvent(Watch.Response<V1Secret> event) {
        if (event.object.getMetadata().getNamespace().equals(selected.getMetadata().getNamespace())
                && event.object.getMetadata().getName().equals(selected.getMetadata().getName())) {
            if (event.type.equals(K8sUtil.WATCH_EVENT_DELETED)) {
                core.ui().access(() -> {
//                    deskTab.closeTab();
                    UiUtil.showErrorNotification("Secret was deleted on server");
                    status.setText("Secret was deleted on server " + event.object.getMetadata().getCreationTimestamp());
                });
            } else {
                selected = event.object;
                core.ui().access(() -> {
                    status.setText("Secret changed on server " + event.object.getMetadata().getDeletionTimestamp());
                    updateEntryList();
                });
            }
        }
    }

    @Override
    public void tabSelected() {

    }

    @Override
    public void tabUnselected() {

    }

    @Override
    public void tabDestroyed() {
        registration.unregister();
    }

    @Override
    public void tabRefresh(long counter) {

    }

    @Override
    public void tabCloseClicked(DeskTab deskTab) {
        boolean changed = false;
        for (int i = 0; i < entryList.getComponentCount(); i++) {
            Entry entry = (Entry) entryList.getComponentAt(i);
            if (entry.changed || entry.newEntry) {
                changed = true;
                break;
            }
        }
        if (changed) {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Entry changed");
            dialog.setText("Changed data will be lost.");
            dialog.setCloseOnEsc(true);
            dialog.setCancelable(true);
            dialog.addConfirmListener(e2 -> {
                deskTab.closeTab();
            });
            dialog.open();
        } else {
            deskTab.closeTab();
        }
    }


    private class Entry extends HorizontalLayout {

        private final Button deleteBtn;
        private final Button editBtn;
        private final Button resetBtn;
        private String key;
        private TextArea valueField = null;
        private TextField keyField = null;
        private String value;
        private boolean changed = false;
        private boolean newEntry = false;
        private boolean showContent = false;

        public Entry(String key, String value) {
            this.key = key;

            setWidthFull();
            setMargin(false);
            setPadding(false);

            var buttonBar = new VerticalLayout();
            buttonBar.setMargin(false);
            buttonBar.setPadding(false);
            buttonBar.setSpacing(false);
            buttonBar.setWidth("40px");
            add(buttonBar);

            editBtn = new Button(LumoIcon.EDIT.create() ,e -> {
                setKeyEditMode(true);
                keyField.setReadOnly(false);
                keyField.focus();
            });
            editBtn.setTooltipText("Edit entry key");
            editBtn.setWidthFull();
            buttonBar.add(editBtn);

            resetBtn = new Button(LumoIcon.RELOAD.create(), e -> {
                showContent = false;
                setChanged(false);
            });
            resetBtn.setTooltipText("Reset entry value");
            resetBtn.setWidthFull();
            buttonBar.add(resetBtn);

            deleteBtn = new Button(LumoIcon.CROSS.create(), e -> {
                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader("Delete entry");
                dialog.setText("Do you really want to delete the entry?");
                dialog.setCloseOnEsc(true);
                dialog.setCancelable(true);
                dialog.addConfirmListener(e2 -> {
                    entryList.remove(Entry.this);
                    dialog.close();
                });
                dialog.open();
            });
            deleteBtn.setTooltipText("Delete entry");
            deleteBtn.setWidthFull();
            buttonBar.add(deleteBtn);

            keyField = new TextField();
            keyField.setValue(key);
            keyField.setReadOnly(true);
            keyField.setWidth("300px");
            keyField.addKeyUpListener(Key.ENTER, e -> {
                // validate
                if (keyField.getValue().isBlank()) {
                    UiUtil.showErrorNotification("Key can not be empty");
                    return;
                }
                if (!keyField.getValue().matches("[a-zA-Z0-9_\\-.]+")) {
                    UiUtil.showErrorNotification("Key can only contain a-z, A-Z, 0-9, . _ and -");
                    return;
                }
                // find new index or deny
                int newIndex = -2;
                for (int i = 0; i < entryList.getComponentCount(); i++) {
                    Entry entry = (Entry)entryList.getComponentAt(i);
                    if (entry == this) continue;
                    if (entry.key.equals(keyField.getValue())) {
                        UiUtil.showErrorNotification("Key already exists");
                        return;
                    }
                    if (entry.key.compareTo(keyField.getValue()) > 0) {
                        newIndex = i-1;
                        break;
                    }
                }
                entryList.remove(this);
                if (newIndex == -1)
                    entryList.addComponentAsFirst(this);
                else
                if (newIndex == -2)
                    entryList.add(this);
                else
                    entryList.addComponentAtIndex(newIndex, this);

                this.key = keyField.getValue();
                setKeyEditMode(false);
                valueField.focus();
            });
            add(keyField);

            valueField = new TextArea();
            valueField.setWidthFull();
            valueField.addFocusListener(e -> {
                if (showContent) return;
                showContent = true;
                updateValueField();
            });
            valueField.addValueChangeListener(e -> setChanged(true));
            add(valueField);

            updateValue(value);
            updateValueField();
        }

        private void updateValueField() {
            if (showContent)
                valueField.setValue(value);
            else
                valueField.setValue("... xxx ...");
        }

        private void setNewEntry(boolean newEntry) {
            this.newEntry = newEntry;
            if (newEntry) {
                changed = true;
                this.addClassName("entry-new");
            } else {
                this.removeClassName("entry-new");
            }
        }

        private void setChanged(boolean changed) {
            if (!showContent) {
                updateValueField();
                this.removeClassName("entry-changed");
                return;
            }
            if (newEntry) return;
            if (changed) {
                this.changed = !valueField.getValue().equals(value);
                if (this.changed) {
                    this.addClassName("entry-changed");
                } else {
                    this.removeClassName("entry-changed");
                }
            } else {
                this.changed = false;
                updateValueField();
                this.removeClassName("entry-changed");
            }
        }

        public void updateValue(String value) {
            this.value = value;
            if (!changed)
                updateValueField();
        }

        public void setEntryKeyEditMode(boolean mode) {
            keyField.setReadOnly(true);
            valueField.setReadOnly(mode);
            editBtn.setEnabled(!mode);
            resetBtn.setEnabled(!mode);
            deleteBtn.setEnabled(!mode);
        }

        public String getValue() {
            if (showContent)
                return valueField.getValue();
            else
                return value;
        }
    }

}
