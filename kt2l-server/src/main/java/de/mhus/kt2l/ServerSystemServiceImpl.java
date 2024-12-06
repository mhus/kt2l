/*
 * kt2l-server - kt2l as server
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
package de.mhus.kt2l;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import de.mhus.commons.tools.MDate;
import de.mhus.commons.tools.MFile;
import de.mhus.commons.tools.MLang;
import de.mhus.kt2l.aaa.AaaUser;
import de.mhus.kt2l.config.Configuration;
import de.mhus.kt2l.system.ServerSystemService;
import de.mhus.kt2l.ui.UiUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
                MLang.tryThis(() -> VaadinServletRequest.getCurrent().getHttpServletRequest().getRemoteHost()).orElse("?") + " " + MLang.tryThis(() -> Collections.list(VaadinServletRequest.getCurrent().getHttpServletRequest().getHeaders("X-Forwarded-For")).toString()).orElse("?"),
                MLang.tryThis(() -> UI.getCurrent().getSession().getBrowser().getBrowserApplication()).orElse("?"),
                MLang.tryThis(() -> UI.getCurrent().getSession().getSession().getId()).orElse("?")
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
