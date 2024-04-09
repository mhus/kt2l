package com.flowingcode.vaadin.addons.xterm.utils;

import elemental.json.Json;
import elemental.json.JsonObject;

public class TerminalKeyMapping {
    public static String map(String payload) {
        JsonObject jsonObject = Json.parse(payload);
        return map(jsonObject);
    }

    public static String map(JsonObject key) {
        String keyName = key.getString("key");
        //XXX check for special keys

        if (keyName.length() == 1) {
            return keyName;
        }

        if (keyName.equals("Enter")) {
            return "\n";
        } else if (keyName.equals("Backspace")) {
            return "\b";
        } else if (keyName.equals("Tab")) {
            return "\t";
        } else if (keyName.equals("Escape")) {
            return "\u001b";
        } else if (keyName.equals("ArrowUp")) {
            return "\u001b[A";
        } else if (keyName.equals("ArrowDown")) {
            return "\u001b[B";
        } else if (keyName.equals("ArrowRight")) {
            return "\u001b[C";
        } else if (keyName.equals("ArrowLeft")) {
            return "\u001b[D";
        } else if (keyName.equals("Delete")) {
            return "\u007f";
        } else if (keyName.equals("Home")) {
            return "\u001b[H";
        } else if (keyName.equals("End")) {
            return "\u001b[F";
        } else if (keyName.equals("Insert")) {
            return "\u001b[2~";
        } else if (keyName.equals("PageUp")) {
            return "\u001b[5~";
        } else if (keyName.equals("PageDown")) {
            return "\u001b[6~";
        } else if (keyName.equals("F1")) {
            return "\u001bOP";
        } else if (keyName.equals("F2")) {
            return "\u001bOQ";
        } else if (keyName.equals("F3")) {
            return "\u001bOR";
        } else if (keyName.equals("F4")) {
            return "\u001bOS";
        } else if (keyName.equals("F5")) {
            return "\u001b[15~";
        } else if (keyName.equals("F6")) {
            return "\u001b[17~";
        } else if (keyName.equals("F7")) {
            return "\u001b[18~";
        } else if (keyName.equals("F8")) {
            return "\u001b[19~";
        } else if (keyName.equals("F9")) {
            return "\u001b[20~";
        } else if (keyName.equals("F10")) {
            return "\u001b[21~";
        } else if (keyName.equals("F11")) {
            return "\u001b[23~";
        } else if (keyName.equals("F12")) {
            return "\u001b[24~";
        }

        return "";  //XXX unknown key
    }
}
