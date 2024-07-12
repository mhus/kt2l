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

package de.mhus.kt2l.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.HasMenuItems;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.server.Command;
import de.mhus.commons.tools.MCast;
import de.mhus.commons.tools.MCollection;
import de.mhus.commons.tools.MJson;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tools.MSystem;
import de.mhus.commons.yaml.MYaml;
import io.kubernetes.client.openapi.ApiException;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static de.mhus.commons.tools.MCollection.cropArray;
import static de.mhus.commons.tools.MString.isEmptyTrim;

@Slf4j
public class UiUtil {

    public static COLOR toColor(String color) {
        if (color == null) return COLOR.NONE;
        try {
            return COLOR.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return COLOR.NONE;
        }
    }

    public static String toShortcutString(String shortcut) {
        if (shortcut == null) return "";
        shortcut = normalizeShortcutString(shortcut);
        if (MSystem.isMac()) {
            shortcut = shortcut.replace("META", "\u2318");
            shortcut = shortcut.replace("ALT", "\u2325");
            shortcut = shortcut.replace("CONTROL", "\u2303");
            shortcut = shortcut.replace("SHIFT", "\u21E7");
            shortcut = shortcut.replace("+", "");
            shortcut = shortcut.replace("BACKSPACE", "\u232B");
            return shortcut;
        } else {
            shortcut = shortcut.replace("CONTROL", "CTRL");
            shortcut = shortcut.replace("META", "WIN");
            shortcut = shortcut.replace("BACKSPACE", "\u232B");
        }
        return "[" + shortcut.toUpperCase() + "]";
    }

    public static Shortcut createShortcut(String shortcutKey) {
        if (isEmptyTrim(shortcutKey)) return null;
        return new Shortcut(shortcutKey);
    }

    public static void showErrorNotification(String msg) {
        showErrorNotification(msg, null);
    }

    public static void showErrorNotification(String msg, Exception e) {
        Notification notification = new Notification();
        Div text = new Div(
                new Text(msg)
        );
        if (e != null) {
            var error = new Div(e.getLocalizedMessage());
            if (e instanceof ApiException apiException) {
                LOGGER.debug(e + ": " + apiException.getCode() + " " + apiException.getResponseBody());
            }
            var detailsBtn = new Button("Details");
            detailsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            detailsBtn.addClickListener(event -> {
                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setCloseOnEsc(true);
                dialog.setConfirmText("Close");
                dialog.setHeader("Error Details");
                dialog.setHeight("80%");
                dialog.setWidth("80%");
                StringBuilder sb = new StringBuilder();
                sb.append(e.getLocalizedMessage()).append("\n\n");
                if (e instanceof ApiException apiException) {
                    sb.append("Error Code: ").append(apiException.getCode()).append("\n\n");
                    var body = apiException.getResponseBody();
                    if (body.startsWith("{")) {
                        try {
                            sb.append(MYaml.toYaml(MJson.load(body))).append("\n\n");
                        } catch (Exception ex) {
                            sb.append(body).append("\n\n");
                        }
                    } else {
                        sb.append(body).append("\n\n");
                    }
                }
                sb.append(MCast.toString(e));
                var textArea = new TextArea();
                textArea.setValue(sb.toString());
                textArea.setSizeFull();
                textArea.setReadOnly(true);
                textArea.addClassName("monotext");
                dialog.setText(textArea);
                dialog.open();
            });
            error.addClassName("error-exception");
            text = new Div(
                    new Div(msg),
                    error,
                    detailsBtn
            );
        }

        notification.add(text);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.setPosition(Notification.Position.TOP_END);
        notification.setDuration(5000);
        notification.open();
    }

    public static void showSuccessNotification(String msg) {
        Notification notification = Notification.show(msg);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.setPosition(Notification.Position.TOP_END);
        notification.setDuration(5000);
    }

    public static String toId(String in) {
        if (in == null) return null;
        return in.replaceAll("[^A-Za-z0-9\\-]", "");
    }

    public static KeyModifier getOSMetaModifier() {
        return MSystem.isMac() ? KeyModifier.META : KeyModifier.CONTROL;
    }

    public static String getOSMetaModifierString() {
        return MSystem.isMac() ? "META" : "CONTROL";
    }

    public static String xtermPrepareEsc(String line) {
        if (line.contains("\b [1Ps\b")) {
            line = line.replaceAll("\b \\[1Ps\b", "\b\u001b[P");
        }
        return line;
    }

    public static byte[] xtermKeyToBytes(String xtermKey) {
        try {
                /*
Key: {"key":"A","code":"KeyA","ctrlKey":false,"altKey":false,"metaKey":false,"shiftKey":true}
Key: {"key":"Meta","code":"MetaLeft","ctrlKey":false,"altKey":false,"metaKey":true,"shiftKey":false}
Key: {"key":"ArrowUp","code":"ArrowUp","ctrlKey":false,"altKey":false,"metaKey":false,"shiftKey":false}
Key: {"key":"Meta","code":"MetaLeft","ctrlKey":false,"altKey":false,"metaKey":true,"shiftKey":false}
Key: {"key":"Meta","code":"MetaLeft","ctrlKey":false,"altKey":false,"metaKey":true,"shiftKey":false}
Key: {"key":"Escape","code":"Escape","ctrlKey":false,"altKey":false,"metaKey":false,"shiftKey":false}
Key: {"key":"Meta","code":"MetaLeft","ctrlKey":false,"altKey":false,"metaKey":true,"shiftKey":false}
                 */
            var json = MJson.load(xtermKey);
            var key = json.get("key").asText();
            if (key.length() == 1) {
                if (json.get("ctrlKey").asBoolean()) {
                    key = key.toUpperCase();
                    if (key.length() == 1) {
                        return new byte[]{(byte) (key.charAt(0) - 'A' + 1)};
                    }
                    LOGGER.warn("Unknown CTRL key: " + key);
                    return null;
                }

                return key.getBytes();
            }

            switch (key) {
                case "ArrowUp": return new byte[]{0x1b, 0x5b, 0x41};
                case "ArrowDown": return new byte[]{0x1b, 0x5b, 0x42};
                case "ArrowRight": return new byte[]{0x1b, 0x5b, 0x43};
                case "ArrowLeft": return new byte[]{0x1b, 0x5b, 0x44};
                case "Escape": return new byte[]{0x1b};
                case "Backspace": return new byte[]{0x7f};
                case "Delete": return new byte[]{0x1b, 0x5b, 0x33, 0x7e};
                case "Enter": return new byte[]{0x0d};
                case "Tab": return new byte[]{0x09};
                case "Meta": return null;
                case "Control": return null;
                case "Shift": return null;
                case "Alt": return null;
            }

            LOGGER.warn("Unknown key: " + key);
            return null;
        } catch (Exception ex) {
            LOGGER.warn("Key", ex);
            return null;
        }

    }

    public static String normalizeId(String id) {
        if (id == null) return null;
        return id.replaceAll("[^A-Za-z0-9\\-]", "");
    }

    @Getter
    @ToString(exclude = "registration")
    public static class Shortcut {

        private final Key key;
        private final KeyModifier[] modifier;
        private ShortcutRegistration registration;

        public Shortcut(Key key, KeyModifier[] modifier) {
            this.key = key;
            this.modifier = modifier;
        }

        public Shortcut(String shortcutKey) {
            shortcutKey=UiUtil.normalizeShortcutString(shortcutKey);
            final var k1 = shortcutKey.split("\\+");
            final var modifierStrings = cropArray(k1, 0, k1.length-1);
            this.modifier = new KeyModifier[modifierStrings.length];
            for (int i = 0; i < modifierStrings.length; i++)
                modifier[i] = KeyModifier.valueOf(modifierStrings[i].toUpperCase());
            if (k1[k1.length-1].equals("DELETE"))
                this.key = Key.DELETE;
            else
            if (k1[k1.length-1].equals("ENTER"))
                this.key = Key.ENTER;
            else
            if (k1[k1.length-1].equals("SPACE"))
                this.key = Key.SPACE;
            else
            if (k1[k1.length-1].equals("TAB"))
                this.key = Key.TAB;
            else
            if (k1[k1.length-1].equals("ESCAPE"))
                this.key = Key.ESCAPE;
            else
            if (k1[k1.length-1].equals("BACKSPACE"))
                this.key = Key.BACKSPACE;
            else
            if (k1[k1.length-1].equals("F1"))
                this.key = Key.F1;
            else
            if (k1[k1.length-1].equals("F2"))
                this.key = Key.F2;
            else
            if (k1[k1.length-1].equals("F3"))
                this.key = Key.F3;
            else
            if (k1[k1.length-1].equals("F4"))
                this.key = Key.F4;
            else
            if (k1[k1.length-1].equals("F5"))
                this.key = Key.F5;
            else
            if (k1[k1.length-1].equals("F6"))
                this.key = Key.F6;
            else
            if (k1[k1.length-1].equals("F7"))
                this.key = Key.F7;
            else
            if (k1[k1.length-1].equals("F8"))
                this.key = Key.F8;
            else
            if (k1[k1.length-1].equals("F9"))
                this.key = Key.F9;
            else
            if (k1[k1.length-1].equals("F10"))
                this.key = Key.F10;
            else
            if (k1[k1.length-1].equals("F11"))
                this.key = Key.F11;
            else
            if (k1[k1.length-1].equals("F12"))
                this.key = Key.F12;
            else
                this.key = Key.of(k1[k1.length-1].toLowerCase());
        }

        public void addShortcutListener(Component target, Command command) {
            if (registration != null) registration.remove();
            if (MCollection.isEmpty(modifier))
                registration = UI.getCurrent().addShortcutListener(command, key).listenOn(target);
            else
                registration = UI.getCurrent().addShortcutListener(command, key).withModifiers(modifier).listenOn(target);
        }
    }

    private static String normalizeShortcutString(String shortcutKey) {
        shortcutKey = shortcutKey.toUpperCase();
        if (MSystem.isMac())
            shortcutKey = shortcutKey.replace("OS", "META");
        else {
            shortcutKey = shortcutKey.replace("OS", "CONTROL");
        }
        return shortcutKey.toUpperCase().replace("CTRL", "CONTROL").replace("WIN", "META");
    }

    public enum COLOR {
        NONE, RED, GREEN, BLUE, YELLOW, ORANGE, PURPLE, CYAN, BLACK, WHITE, PINK, MAGENTA, BROWN, GREY
    }

    public static List<COLOR> LIGHT_COLORS = List.of(COLOR.WHITE, COLOR.RED, COLOR.GREEN, COLOR.BLUE, COLOR.YELLOW, COLOR.ORANGE, COLOR.PURPLE, COLOR.PINK, COLOR.MAGENTA);

    public static MenuItem createIconItem(HasMenuItems menu, VaadinIcon iconName, String label, String ariaLabel) {
        return createIconItem(menu, iconName, label, ariaLabel, false);
    }
    public static MenuItem createIconItem(HasMenuItems menu, VaadinIcon iconName, String label, String ariaLabel, boolean isChild) {
        Icon icon = new Icon(iconName);

        if (isChild) {
            icon.getStyle().set("width", "var(--lumo-icon-size-s)");
            icon.getStyle().set("height", "var(--lumo-icon-size-s)");
            icon.getStyle().set("marginRight", "var(--lumo-space-s)");
        }

        MenuItem item = menu.addItem(icon, e -> {
        });

        if (ariaLabel != null) {
            item.getElement().setAttribute("aria-label", ariaLabel);
        }

        if (label != null) {
            item.add(new Text(label));
        }

        return item;
    }
}
