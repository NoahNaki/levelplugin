package me.nakilex.levelplugin.particles.util;

public final class Validate {
    private Validate() {
    }

    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void isTrue(boolean expression, String message, Object value) {
        if (!expression) {
            throw new IllegalArgumentException(message + value);
        }
    }

    public static <T> T notNull(T object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
        return object;
    }
}
