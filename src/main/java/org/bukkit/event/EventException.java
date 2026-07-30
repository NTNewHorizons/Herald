package org.bukkit.event;

public class EventException extends Exception {
    public EventException(String message) {
        super(message);
    }

    public EventException(String message, Throwable cause) {
        super(message, cause);
    }
}
