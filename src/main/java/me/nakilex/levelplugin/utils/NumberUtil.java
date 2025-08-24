package me.nakilex.levelplugin.utils;

import java.text.NumberFormat;
import java.util.Locale;

/** Utility methods for number formatting. */
public final class NumberUtil {
    private NumberUtil() {}

    /** Format a number with comma separators using US locale. */
    public static String formatCommas(long value) {
        return NumberFormat.getNumberInstance(Locale.US).format(value);
    }
}
