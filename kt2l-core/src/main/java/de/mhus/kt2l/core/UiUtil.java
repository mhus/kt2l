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

package de.mhus.kt2l.core;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.Command;
import de.mhus.commons.tools.MCollection;
import de.mhus.commons.tools.MSystem;
import lombok.Getter;

import java.util.List;

import static de.mhus.commons.tools.MCollection.cropArray;
import static de.mhus.commons.tools.MString.isEmptyTrim;

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
            shortcut = shortcut.replace("DELETE", "\u232B");
            return shortcut;
        } else {
            shortcut = shortcut.replace("CONTROL", "CTRL");
            shortcut = shortcut.replace("META", "WIN");
            shortcut = shortcut.replace("DELETE", "\u232B");
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
            var error = new Div(e.toString());
            error.addClassName("error-exception");
            text = new Div(
                    new Div(msg),
                    error
            );
        }
        notification.add(text);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.setPosition(Notification.Position.TOP_START);
        notification.setDuration(5000);
        notification.open();
    }

    public static void showSuccessNotification(String msg) {
        Notification notification = Notification.show(msg);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.setPosition(Notification.Position.TOP_START);
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

    @Getter
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
}
