package me.nakilex.levelplugin.fishing.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class FishingArgs {
    private FishingArgs() {
    }

    public static String getString(Map<String, Object> args, String key) {
        Object value = args.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public static String getString(Map<String, Object> args, String key, String fallback) {
        String value = getString(args, key);
        return value == null ? fallback : value;
    }

    public static int getInt(Map<String, Object> args, String key, int fallback) {
        Object value = args.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    public static double getDouble(Map<String, Object> args, String key, double fallback) {
        Object value = args.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    public static boolean getBoolean(Map<String, Object> args, String key, boolean fallback) {
        Object value = args.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value != null) {
            return Boolean.parseBoolean(value.toString());
        }
        return fallback;
    }

    public static List<String> getStringList(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value instanceof List<?> list) {
            List<String> output = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    output.add(item.toString());
                }
            }
            return output;
        }
        if (value instanceof String str) {
            String[] parts = str.split(",");
            List<String> output = new ArrayList<>();
            for (String part : parts) {
                if (!part.isBlank()) {
                    output.add(part.trim());
                }
            }
            return output;
        }
        return Collections.emptyList();
    }
}
