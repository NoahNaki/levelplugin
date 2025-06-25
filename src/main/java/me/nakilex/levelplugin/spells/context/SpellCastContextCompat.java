package me.nakilex.levelplugin.spells.context;

import java.lang.reflect.Method;

/**
 * Compatibility helper for {@link SpellCastContext}.
 * Gracefully handles older versions lacking markSuccess(boolean).
 */
public final class SpellCastContextCompat {
    private static final Method MARK_SUCCESS;

    static {
        Method m = null;
        try {
            m = SpellCastContext.class.getMethod("markSuccess", boolean.class);
        } catch (NoSuchMethodException ignore) {
            // Method absent on older API versions
        }
        MARK_SUCCESS = m;
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
}
