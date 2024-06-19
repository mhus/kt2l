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
package de.mhus.kt2l.cluster;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.ShortcutEvent;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.html.Image;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class KonamiAction {

    private static final Key[] KONAMI = { Key.ARROW_UP, Key.ARROW_UP, Key.ARROW_DOWN, Key.ARROW_DOWN, Key.ARROW_LEFT,
            Key.ARROW_RIGHT, Key.ARROW_LEFT, Key.ARROW_RIGHT, Key.KEY_B, Key.KEY_A};
    private int index = 0;
    private List<ShortcutRegistration> shortcutRegistrations = Collections.synchronizedList(new ArrayList<>());

    public void attach(Component component) {
        LOGGER.debug("Attach Konami: {}", component);
        component.addAttachListener(e -> {
            shortcutRegistrations.forEach(ShortcutRegistration::remove);
            addShortcutRegistration(Shortcuts.addShortcutListener(component, this::konami, Key.ARROW_UP, KeyModifier.ALT));
            addShortcutRegistration(Shortcuts.addShortcutListener(component, this::konami, Key.ARROW_DOWN, KeyModifier.ALT));
            addShortcutRegistration(Shortcuts.addShortcutListener(component, this::konami, Key.ARROW_LEFT, KeyModifier.ALT));
            addShortcutRegistration(Shortcuts.addShortcutListener(component, this::konami, Key.ARROW_RIGHT, KeyModifier.ALT));
            addShortcutRegistration(Shortcuts.addShortcutListener(component, this::konami, Key.KEY_B, KeyModifier.ALT));
            addShortcutRegistration(Shortcuts.addShortcutListener(component, this::konami, Key.KEY_A, KeyModifier.ALT));
        });
    }

    private void addShortcutRegistration(ShortcutRegistration shortcutRegistration) {
        shortcutRegistrations.add(shortcutRegistration);
    }

    private void konami(ShortcutEvent event) {
        // LOGGER.debug("Konami {}", event.getKey().getKeys());
        if (event.getKey().getKeys().getFirst().equals(KONAMI[index].getKeys().getFirst())) {
            index++;
            // LOGGER.debug("Konami {}", index);
            if (index == KONAMI.length) {
                index = 0;
                LOGGER.debug("Konami done");
                doAction();
            }
        } else {
            index = 0;
        }
    }

    public void doAction() {
    }
}
