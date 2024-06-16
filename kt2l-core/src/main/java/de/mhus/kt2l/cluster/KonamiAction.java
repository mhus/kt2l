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
