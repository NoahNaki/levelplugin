package me.nakilex.levelplugin.dialogue.render;

import me.nakilex.levelplugin.utils.ChatFormatter;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formatting helpers for dialogue HUD text inserted into MiniMessage strings.
 */
public final class DialoguePlaceholderFormatter {
    private static final Pattern HEX_COLOR = Pattern.compile("(?i)^#([0-9a-f]{6})$");

    private DialoguePlaceholderFormatter() {
    }

    public static String miniMessageText(String text) {
        return escapeMiniMessage(plainText(text));
    }

    public static String plainText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return stripLegacyCodes(ChatFormatter.colorize(text));
    }

    public static String miniMessageColor(String color, String fallback) {
        String normalizedFallback = fallback == null || fallback.isBlank() ? "#ffffff" : fallback.trim();
        if (color == null || color.isBlank()) {
            return normalizedFallback;
        }

        String trimmed = color.trim();
        Matcher hex = HEX_COLOR.matcher(trimmed);
        if (hex.matches()) {
            return "#" + hex.group(1).toLowerCase(Locale.ROOT);
        }

        String colorized = ChatFormatter.colorize(trimmed);
        String legacyHex = firstLegacyHex(colorized);
        if (legacyHex != null) {
            return legacyHex;
        }

        String legacyColor = firstLegacyColor(colorized);
        return legacyColor == null ? normalizedFallback : legacyColor;
    }

    private static String stripLegacyCodes(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }
            result.append(character);
        }
        return result.toString();
    }

    private static String firstLegacyHex(String text) {
        for (int i = 0; i + 13 < text.length(); i++) {
            if (text.charAt(i) != '§' || Character.toLowerCase(text.charAt(i + 1)) != 'x') {
                continue;
            }
            StringBuilder hex = new StringBuilder("#");
            boolean valid = true;
            for (int j = 0; j < 6; j++) {
                int sectionIndex = i + 2 + (j * 2);
                int digitIndex = sectionIndex + 1;
                if (text.charAt(sectionIndex) != '§' || !isHexDigit(text.charAt(digitIndex))) {
                    valid = false;
                    break;
                }
                hex.append(Character.toLowerCase(text.charAt(digitIndex)));
            }
            if (valid) {
                return hex.toString();
            }
        }
        return null;
    }

    private static String firstLegacyColor(String text) {
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) != '§') {
                continue;
            }
            String color = legacyColor(Character.toLowerCase(text.charAt(i + 1)));
            if (color != null) {
                return color;
            }
        }
        return null;
    }

    private static String legacyColor(char code) {
        return switch (code) {
            case '0' -> "#000000";
            case '1' -> "#0000aa";
            case '2' -> "#00aa00";
            case '3' -> "#00aaaa";
            case '4' -> "#aa0000";
            case '5' -> "#aa00aa";
            case '6' -> "#ffaa00";
            case '7' -> "#aaaaaa";
            case '8' -> "#555555";
            case '9' -> "#5555ff";
            case 'a' -> "#55ff55";
            case 'b' -> "#55ffff";
            case 'c' -> "#ff5555";
            case 'd' -> "#ff55ff";
            case 'e' -> "#ffff55";
            case 'f' -> "#ffffff";
            default -> null;
        };
    }

    private static boolean isHexDigit(char character) {
        return (character >= '0' && character <= '9')
                || (character >= 'a' && character <= 'f')
                || (character >= 'A' && character <= 'F');
    }

    private static String escapeMiniMessage(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("<", "\\<");
    }
}
