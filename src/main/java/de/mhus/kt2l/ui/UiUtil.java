package de.mhus.kt2l.ui;

public class UiUtil {

    public static COLOR toColor(String color) {
        if (color == null) return COLOR.NONE;
        try {
            return COLOR.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return COLOR.NONE;
        }
    }

    public enum COLOR {
        NONE, RED, GREEN, BLUE, YELLOW, ORANGE, PURPLE, CYAN, BLACK, WHITE, PINK, MAGENTA, BROWN
    }
}
