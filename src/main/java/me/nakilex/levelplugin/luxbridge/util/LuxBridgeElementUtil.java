package me.nakilex.levelplugin.luxbridge.util;

public final class LuxBridgeElementUtil {
    public static final String NEGATIVE_OFFSET = "七";
    public static final String POSITIVE_OFFSET = "𥳾";
    public static final String NAMESPACE = "levelplugin_dialogue";

    private LuxBridgeElementUtil() {}

    public static String getOffset(float offset) {
        if (offset == 0.0f) return "";
        String character = offset < 0.0f ? NEGATIVE_OFFSET : POSITIVE_OFFSET;
        return character.repeat((int) Math.ceil(Math.abs(offset)));
    }

    public static String offset(float offset) {
        String chars = getOffset(offset);
        return chars.isEmpty() ? "" : font(NAMESPACE + ":offset_chars", chars);
    }

    public static String font(String font, String content) {
        return "<font:" + font + ">" + content + "</font>";
    }

    public static String colorOpen(String color) {
        return "<color:" + LuxBridgeFormat.color(color, "#ffffff") + ">";
    }

    public static String colorClose() {
        return "</color>";
    }

    public static String coloredFont(String color, String font, String content) {
        return colorOpen(color) + font(font, content) + colorClose();
    }
}
