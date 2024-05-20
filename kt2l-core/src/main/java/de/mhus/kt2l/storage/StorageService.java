package de.mhus.kt2l.storage;

import com.vaadin.flow.component.icon.VaadinIcon;
import de.mhus.commons.errors.NotFoundRuntimeException;
import de.mhus.commons.util.SoftHashMap;
import de.mhus.kt2l.config.CmdConfiguration;
import de.mhus.kt2l.config.UsersConfiguration;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.PanelService;
import de.mhus.kt2l.core.SecurityContext;
import de.mhus.kt2l.core.SecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class StorageService {

    private static final BucketDriver DUMMY_DRIVER = new DummyDriver();
    @Autowired
    private StorageConfiguration storageConfiguration;

    @Autowired
    private List<BucketDriver> drivers;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private CmdConfiguration cmdConfiguration;

    @Autowired
    private PanelService panelService;

    private final SoftHashMap<String, Storage> cache = new SoftHashMap<>();

    public boolean isEnabled() {
        return storageConfiguration.isEnabled();
    }

    public Storage getStorage() {
        if (!isEnabled())
            return DUMMY_DRIVER.createStorage(null, null);

        final var userName = SecurityContext.lookupUserName();
        final var bucket = storageConfiguration.getBucketForUser(userName);
        synchronized (cache) {
            return cache.computeIfAbsent(userName, k -> createStorage(bucket, userName));
        }
    }

    private Storage createStorage(StorageConfiguration.Bucket bucket, String userName) {
        for (var driver : drivers) {
            if (driver.supports(bucket)) {
                return driver.createStorage(bucket, userName);
            }
        }
        throw new NotFoundRuntimeException("No driver found for " + bucket);
    }

    public boolean open(StorageFile file) {
        if (!file.getStorage().isLocal() || !securityService.hasRole(UsersConfiguration.ROLE.LOCAL.name()))
            return false;
        try {
            final var path = file.getStorage().getLocalPath(file);
            LOGGER.debug("open {} for file {}", path, file);
            cmdConfiguration.openFileBrowser(file.getStorage().getLocalPath(file));
            return true;
        } catch (Exception e) {
            LOGGER.warn("open {} failed", file, e);
            return false;
        }
    }

    public void showStoragePanel(Core core, StorageFile file) {
        if (!isEnabled()) return;

        var tab = panelService.addPanel(
                core,
                null,
                "storage",
                "Storage",
                true,
                VaadinIcon.STORAGE.create(),
                () -> new StoragePanel()).select();
        if (file != null) {
            ((StoragePanel)tab.getPanel()).showFile(file);
        }
    }

}
