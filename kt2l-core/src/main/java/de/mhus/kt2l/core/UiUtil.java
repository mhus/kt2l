package de.mhus.kt2l.core;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.server.Command;
import de.mhus.commons.tools.MSystem;
import lombok.Getter;

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
        shortcut = shortcut.toUpperCase();
        if (MSystem.isMac()) {
            shortcut = shortcut.replace("META", "\u2318");
            shortcut = shortcut.replace("ALT", "\u2325");
            shortcut = shortcut.replace("CONTROL", "\u2303");
            shortcut = shortcut.replace("SHIFT", "\u21E7");
            shortcut = shortcut.replace("+", "");
            return shortcut;
        } else {
            shortcut = shortcut.replace("CONTROL", "CTRL");
            shortcut = shortcut.replace("META", "WIN");
        }
        return "[" + shortcut.toUpperCase() + "]";
    }

    public static Shortcut createShortcut(String shortcutKey) {
        if (isEmptyTrim(shortcutKey)) return null;
        final var k1 = shortcutKey.split("\\+");
        final var modifierStrings = cropArray(k1, 0, k1.length-1);
        final var modifier = new KeyModifier[modifierStrings.length];
        for (int i = 0; i < modifierStrings.length; i++)
            modifier[i] = KeyModifier.valueOf(modifierStrings[i].toUpperCase());
        final var key = Key.of(k1[k1.length-1].toLowerCase());
        if (key != null) return new Shortcut(key, modifier);
        return null;
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

        public void addShortcutListener(Component target, Command command) {
            if (registration != null) registration.remove();
            registration = UI.getCurrent().addShortcutListener(command, key).withModifiers(modifier).listenOn(target);
        }
    }

    public enum COLOR {
        NONE, RED, GREEN, BLUE, YELLOW, ORANGE, PURPLE, CYAN, BLACK, WHITE, PINK, MAGENTA, BROWN
    }
}
