package com.ntnh.herald.auth;

public final class LoginDecision {

    private static final LoginDecision ALLOWED = new LoginDecision(true, "");

    private final boolean allowed;
    private final String kickMessage;

    private LoginDecision(boolean allowed, String kickMessage) {
        this.allowed = allowed;
        this.kickMessage = kickMessage;
    }

    public static LoginDecision allow() {
        return ALLOWED;
    }

    public static LoginDecision reject(String kickMessage) {
        return new LoginDecision(false, kickMessage == null ? "" : kickMessage);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getKickMessage() {
        return kickMessage;
    }
}
