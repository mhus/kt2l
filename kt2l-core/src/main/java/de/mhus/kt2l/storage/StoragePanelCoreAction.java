package de.mhus.kt2l.storage;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.CoreAction;
import de.mhus.kt2l.core.PanelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StoragePanelCoreAction implements CoreAction {

    @Autowired
    private StorageConfiguration storageConfiguration;

    @Autowired
    private PanelService panelService;

    @Override
    public boolean canHandle(Core core) {
        return storageConfiguration.isEnabled();
    }

    @Override
    public String getTitle() {
        return "Storage";
    }

    @Override
    public void execute(Core core) {
        panelService.addPanel(
                core,
                null,
                "storage",
                "Storage",
                true,
                VaadinIcon.STORAGE.create(),
                () -> new StoragePanel()).select();
    }

    @Override
    public Icon getIcon() {
        return VaadinIcon.STORAGE.create();
    }

    @Override
    public int getPriority() {
        return 1000;
    }
}