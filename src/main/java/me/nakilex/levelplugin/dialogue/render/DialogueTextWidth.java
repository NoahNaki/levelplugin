package me.nakilex.levelplugin.dialogue.render;

/**
 * Small Minecraft-font width approximation for dialogue HUD rewinds.
 */
public final class DialogueTextWidth {
    private DialogueTextWidth() {
    }

    public static int width(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int width = 0;
        boolean colorCode = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '§' || c == '&') {
                colorCode = true;
                continue;
            }

            if (colorCode) {
                colorCode = false;
                continue;
            }

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
