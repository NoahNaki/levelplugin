package me.nakilex.levelplugin.spells.context;

import java.lang.reflect.Method;

/**
 * Compatibility helper for {@link SpellCastContext}.
 * Gracefully handles older versions lacking markSuccess(boolean)
 * and wasSuccessful().
 */
public final class SpellCastContextCompat {
    private static final Method MARK_SUCCESS;
    private static final Method WAS_SUCCESSFUL;

    static {
        Method m = null;
        try {
            m = SpellCastContext.class.getMethod("markSuccess", boolean.class);
        } catch (NoSuchMethodException ignore) {
            // Method absent on older API versions
        }
        MARK_SUCCESS = m;

        Method ws = null;
        try {
            ws = SpellCastContext.class.getMethod("wasSuccessful");
        } catch (NoSuchMethodException ignore) {
            // Absent on older API versions
        }
        WAS_SUCCESSFUL = ws;
    }

    private SpellCastContextCompat() {}

    /**
     * Invoke ctx.markSuccess(value) if available.
     */
    public static void markSuccess(SpellCastContext ctx, boolean value) {
        if (MARK_SUCCESS != null) {
            try {
                MARK_SUCCESS.invoke(ctx, value);
            } catch (Exception ignore) {
                // Ignore reflection failures
            }
        }
    }

    /**
     * Retrieve ctx.wasSuccessful() if available.
     * Defaults to {@code true} when the method is absent.
     */
    public static boolean wasSuccessful(SpellCastContext ctx) {
        if (WAS_SUCCESSFUL != null) {
            try {
                return (Boolean) WAS_SUCCESSFUL.invoke(ctx);
            } catch (Exception ignore) {
                // Ignore reflection failures
            }
        }
        return true;
    }
}
