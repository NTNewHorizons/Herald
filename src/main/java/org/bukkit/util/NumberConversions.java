package org.bukkit.util;

public final class NumberConversions {

    public static int toInt(Object object) {
        if (object instanceof Number) {
            return ((Number) object).intValue();
        }
        try {
            return Integer.parseInt(object.toString());
        } catch (NumberFormatException | NullPointerException e) {
            return 0;
        }
    }

    public static double square(double value) {
        return value * value;
    }
}
