package de.mhus.kt2l;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vaadin.flow.component.UI;
import de.mhus.commons.tools.MDate;
import de.mhus.commons.tools.MFile;
import de.mhus.commons.tools.MLang;
import de.mhus.kt2l.aaa.AaaUser;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.system.ServerSystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class ServerSystemServiceImpl implements ServerSystemService {

    @Value("${kt2l.storage.directory.home:target/storage/}")
    private String home;
    private File directory;
    final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @PostConstruct
    public void init() {
        directory = new File(home + "/access_log");
        directory.mkdirs();
    }

    @Override
    public void newLogin(AaaUser user) {
        var record = new Access(user.getUserId(), System.currentTimeMillis(),
                MLang.tryThis(() -> UI.getCurrent().getSession().getBrowser().getLocale().toString()).orElse("?"),
                MLang.tryThis(() -> UI.getCurrent().getSession().getBrowser().getAddress()).orElse("?"),
                MLang.tryThis(() -> UI.getCurrent().getSession().getBrowser().getBrowserApplication()).orElse("?")
                );
        var file = new File(directory, MDate.toIso8601(record.time()) + "_" + UUID.randomUUID() + ".log" );
        try {
            var serialized = mapper.writeValueAsString(record);
            MFile.writeFile(file, serialized);
        } catch (Exception e) {
            LOGGER.info("Can't write access log {}", file, e);
        }
    }

    @Override
    public List<Access> getAccessList() {
        var list = new ArrayList<Access>();
        for (var file : Objects.requireNonNull(directory.listFiles(f -> f.isFile() && f.getName().endsWith(".log")))) {
            try {
                var content = MFile.readFile(file);
                var record = mapper.readValue(content, Access.class);
                list.add(record);
            } catch (Exception e) {
                LOGGER.info("Can't read access log {}", file, e);
            }
        }
        return list;
    }

    @Override
    public void clearAccessList() {
        LOGGER.debug("Clear access log");
        for (var file : Objects.requireNonNull(directory.listFiles(f -> f.isFile() && f.getName().endsWith(".log")))) {
            try {
                file.delete();
            } catch (Exception e) {
                LOGGER.info("Can't clear access log {}", file, e);
            }
        }
    }

}
