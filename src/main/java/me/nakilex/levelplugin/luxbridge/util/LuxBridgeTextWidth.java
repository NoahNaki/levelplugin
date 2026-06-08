package me.nakilex.levelplugin.luxbridge.util;

public final class LuxBridgeTextWidth {
    private LuxBridgeTextWidth() {}

    public static int getWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        int width = 0;
        boolean code = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' || c == '&') { code = true; continue; }
            if (code) { code = false; continue; }
            width += switch (c) {
                case ' ', 'i', 'l', '!', '.', ',', ':', ';', '\'' -> 2;
                case 'I', '[', ']', 't' -> 4;
                case 'f', 'k' -> 5;
                default -> 6;
            };
        }
        return width;
    }
}
