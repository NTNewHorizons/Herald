package org.bukkit;

import java.util.Date;
import java.util.Set;

public interface BanList {

    enum Type {
        NAME,
        IP
    }

    Set<BanEntry> getBanEntries();

    boolean isBanned(String target);

    void addBan(String target, String reason, Date expires, String source);

    void pardon(String target);
}
