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

package de.mhus.kt2l.help;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import de.mhus.commons.tools.MLang;
import de.mhus.kt2l.core.Core;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;


@Slf4j
public class HelpUtil {
    private volatile static boolean editMode;
    private static Button editButton;

    public static Optional<HelpResourceConnector> getHelpResourceConnector(Core core) {
        return Optional.ofNullable(
                MLang.tryThis(() -> {
                    var selectedPanel = core.getTabBar().getSelectedTab().getPanel();
                    if (selectedPanel instanceof HelpResourceConnector connector) {
                        return connector;
                    }
                    return null;
                })
                .onFailure(e -> LOGGER.debug("Failed to get HelpResourceConnector from selected panel", e))
                .orElse(null)
        );
    }

    public static void setResourceContent(Core core, String newContent) {
        if (newContent == null) return;
        getHelpResourceConnector(core).ifPresent(connector -> {

            var current = connector.getHelpContent();
            if (current == null) return;

            var diff = calculateDiff(current, newContent);

            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Import Content?");

            AceEditor aceDiff = new AceEditor();
            aceDiff.setTheme(AceTheme.terminal);
            aceDiff.setMode(AceMode.diff);
            aceDiff.setReadOnly(true);
            aceDiff.setValue(diff);
            aceDiff.setSizeFull();

            AceEditor aceEdit = new AceEditor();
            aceEdit.setTheme(AceTheme.terminal);
            aceEdit.setMode(AceMode.text);
            aceEdit.setReadOnly(false);
            aceEdit.setValue(newContent);
            aceEdit.setSizeFull();
            editMode = false;

            dialog.add(aceDiff);
            dialog.setWidth("80%");
            dialog.setHeight("80%");

            editButton = new Button("Edit", e -> {
                dialog.removeAll();
                dialog.add(aceEdit);
                editMode = true;
                editButton.setEnabled(false);
            });
            dialog.getFooter().add(editButton);

            Button cancelButton = new Button("Cancel", e -> {
                dialog.close();
                core.getUI().get().remove(dialog);
            });
            dialog.getFooter().add(cancelButton);

            Button useButton = new Button("Use", e -> {
                dialog.close();
                core.getUI().get().remove(dialog);
                if (editMode)
                    connector.setHelpContent(aceEdit.getValue());
                else
                    connector.setHelpContent(newContent);
            });
            useButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
            useButton.addClickShortcut(Key.ENTER);
            dialog.getFooter().add(useButton);

            dialog.open();

        });
    }

    private static String calculateDiff(String current, String newContent) {

        Patch<String> patch = DiffUtils.diff(List.of(current.split("\n")), List.of(newContent.split("\n")), true);

        StringBuilder out = new StringBuilder();
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            switch (delta.getType()) {
                case DELETE:
                    out.append("@@ ").append(delta.getSource().getPosition()).append(",").append(delta.getSource().size()).append(" @@\n");
                    delta.getSource().getLines().forEach(l -> {
                        out.append("- ");
                        out.append(l);
                        out.append("\n");
                    });
                    break;
                case INSERT:
                    out.append("@@ ").append(delta.getTarget().getPosition()).append(",").append(delta.getTarget().size()).append(" @@\n");
                    delta.getTarget().getLines().forEach(l -> {
                        out.append("+ ");
                        out.append(l);
                        out.append("\n");
                    });
                    break;
                case CHANGE:
                    out.append("@@ ").append(delta.getSource().getPosition()).append(",").append(delta.getSource().size()).append(" ").append(delta.getTarget().getPosition()).append(",").append(delta.getTarget().size()).append(" @@\n");
                    delta.getSource().getLines().forEach(l -> {
                        out.append("- ");
                        out.append(l);
                        out.append("\n");
                    });
                    delta.getTarget().getLines().forEach(l -> {
                        out.append("+ ");
                        out.append(l);
                        out.append("\n");
                    });
                    break;
                case EQUAL:
                    delta.getSource().getLines().forEach(l -> {
                        out.append(l);
                        out.append("\n");
                    });
                    break;
            }
        }
        return out.toString();
    }

}
