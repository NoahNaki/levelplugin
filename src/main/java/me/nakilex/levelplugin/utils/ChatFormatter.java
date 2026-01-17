package me.nakilex.levelplugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFormatter {

    private static final int CENTER_PX = 154; // This is approximately the center of the Minecraft chat window
    public static final int GLYPH_PX = 8;     // approximate width of custom glyphs

    // Matches "#47b587" or "&#47b587" (case-insensitive).
    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)(&)?#([0-9a-f]{6})");

    public static void sendCenteredMessage(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        player.sendMessage(getCenteredText(message));
    }

    /**
     * Return the provided message padded so that it appears centered
     * in chat. This does not send anything to the player.
     */
    public static String getCenteredText(String message) {
        return getCenteredText(message, CENTER_PX);
    }

    /**
     * Return the provided message padded so that it appears centered based on
     * a custom pixel width. This is useful for centering text in contexts other
     * than the standard chat window.
     *
     * @param message  the text to center
     * @param centerPx the pixel position that represents the visual center
     * @return the message prefixed with spaces to appear centered
     */
    public static String getCenteredText(String message, int centerPx) {
        if (message == null || message.isEmpty()) return "";

        // Important: convert hex codes before measuring pixel width,
        // otherwise "#RRGGBB" characters distort centering.
        String processed = colorize(message);

        int messagePxSize = pixelLength(processed);

        int toCompensate = centerPx - (messagePxSize / 2);
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while (compensated < toCompensate) {
            sb.append(' ');
            compensated += spaceLength;
        }
        return sb + processed;
    }

    /** Calculate pixel width of a message accounting for color codes and glyph placeholders. */
    public static int pixelLength(String text) {
        if (text == null || text.isEmpty()) return 0;

        int px = 0;
        boolean previousCode = false;
        boolean bold = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '§') {
                previousCode = true;
                continue;
            }
            if (previousCode) {
                previousCode = false;
                bold = (c == 'l' || c == 'L');
                continue;
            }

            // Your custom glyph placeholder format: <glyph:...>
            if (text.startsWith("<glyph:", i)) {
                int end = text.indexOf('>', i);
                if (end == -1) end = text.length() - 1;
                i = end;
                px += GLYPH_PX + 1;
                continue;
            }

            DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
            px += (bold ? DefaultFontInfo.getBoldLength() : dFI.getLength()) + 1;
        }
        return px;
    }

    public static void constructDivider(Player player, String dividerChar, int length) {
        StringBuilder divider = new StringBuilder();
        for (int i = 0; i < length; i++) {
            divider.append(dividerChar);
        }
        player.sendMessage(divider.toString());
    }

    /**
     * Send a line of chat with a small indentation.
     */
    public static void sendIndentedMessage(Player player, String message) {
        if (message == null) return;
        player.sendMessage("        " + message);
    }

    /**
     * Apply legacy &-color codes AND hex colors (#RRGGBB or &#RRGGBB) into
     * Minecraft legacy section codes.
     *
     * This is safe: invalid hex won't throw; it will just be left untouched.
     */
    public static String colorize(String input) {
        if (input == null || input.isEmpty()) return "";
        String withHex = applyHex(input);
        return ChatColor.translateAlternateColorCodes('&', withHex);
    }

    private static String applyHex(String input) {
        try {
            Matcher matcher = HEX_PATTERN.matcher(input);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String hex = matcher.group(2); // RRGGBB
                String replacement = toLegacyHex(hex);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }

            matcher.appendTail(sb);
            return sb.toString();
        } catch (Throwable t) {
            // Never let formatting kill plugin enable.
            return input;
        }
    }

    private static String toLegacyHex(String hex) {
        // Build "§x§R§R§G§G§B§B"
        StringBuilder out = new StringBuilder("§x");
        for (int i = 0; i < 6; i++) {
            out.append('§').append(hex.charAt(i));
        }
        return out.toString();
    }

    /** Hex color used for experience text and amounts. */
    private static final String EXP_COLOR = colorize("#47b587");

    /** Return the standard colored label used for experience amounts. */
    public static String experienceLabel() {
        return EXP_COLOR + "EXP";
    }

    /** Return the color code used for experience numbers. */
    public static String experienceColor() {
        // Display XP amounts in gray so reward numbers stand out
        // uniformly across coins, gems, and experience.
        return ChatColor.GRAY.toString();
    }

    /**
     * Send multiple centered lines wrapped in a colored divider.
     *
     * @param player the target player
     * @param dividerColor the color code for the divider (e.g. "§a")
     * @param lines the messages to center between the dividers
     */
    public static void sendBoxedCenteredMessages(Player player, String dividerColor, String... lines) {
        constructDivider(player, dividerColor + "§l-", 45);
        for (String line : lines) {
            sendCenteredMessage(player, line);
        }
        constructDivider(player, dividerColor + "§l-", 45);
    }
}
