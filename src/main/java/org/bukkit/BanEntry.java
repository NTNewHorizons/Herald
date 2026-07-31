package org.bukkit;

import java.util.Date;

public class BanEntry {

    private final String target;
    private final Date created;
    private String reason;
    private Date expiration;
    private String source;

    public BanEntry(String target) {
        this.target = target;
        this.created = new Date();
    }

    public String getTarget() {
        return target;
    }

    public Date getCreated() {
        return created;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
