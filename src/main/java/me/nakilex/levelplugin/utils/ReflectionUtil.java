package me.nakilex.levelplugin.utils;

/**
 * Utility methods for safely invoking methods via reflection.
 */
public final class ReflectionUtil {
    private ReflectionUtil() {
    }

    /**
     * Attempts to invoke the first available no-arg method from the provided
     * list on the given object. Returns {@code null} if none exist or if
     * invocation fails.
     */
    public static Object invoke(Object obj, String... methods) {
        if (obj == null) return null;
        for (String m : methods) {
            try {
                return obj.getClass().getMethod(m).invoke(obj);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
