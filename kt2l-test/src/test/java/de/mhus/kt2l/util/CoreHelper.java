package de.mhus.kt2l.util;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.CoreListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CoreHelper implements CoreListener {

    @Getter
    private Core lastCore;

    @Override
    public void onCoreCreated(Core core) {
        LOGGER.info("Core created");
        lastCore = core;
    }

    @Override
    public void onCoreDestroyed(Core core) {
    }
}
