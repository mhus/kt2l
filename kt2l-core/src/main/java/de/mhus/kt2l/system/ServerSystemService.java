package de.mhus.kt2l.system;

import de.mhus.kt2l.aaa.AaaUser;
import de.mhus.kt2l.aaa.UsersConfiguration;

import java.util.List;

public interface ServerSystemService {

    void newLogin(AaaUser user);

    List<Access> getAccessList();

    record Access(String name, long time, String locale, String address, String browser) {
    }
}
