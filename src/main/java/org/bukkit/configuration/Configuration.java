package org.bukkit.configuration;

import java.util.Map;

public class Configuration {

    private Map<String, Object> values = new java.util.LinkedHashMap<>();

    public void set(String path, Object value) {
        values.put(path, value);
    }

    public Object get(String path) {
        return values.get(path);
    }

    public String getString(String path) {
        Object v = values.get(path);
        return v != null ? v.toString() : null;
    }

    public int getInt(String path) {
        Object v = values.get(path);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    public boolean getBoolean(String path) {
        Object v = values.get(path);
        return v instanceof Boolean ? (Boolean) v : false;
    }

    public boolean contains(String path) {
        return values.containsKey(path);
    }

    public boolean isSet(String path) {
        return values.containsKey(path);
    }

    public void addDefault(String path, Object value) {
        if (!values.containsKey(path)) {
            values.put(path, value);
        }
    }
}
