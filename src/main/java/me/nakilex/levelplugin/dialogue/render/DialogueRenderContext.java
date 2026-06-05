package me.nakilex.levelplugin.dialogue.render;

import me.nakilex.levelplugin.dialogue.model.DialogueDefinition;
import me.nakilex.levelplugin.dialogue.model.DialoguePage;

import java.util.Locale;
import java.util.Map;

/**
 * Immutable inputs for rendering a single static dialogue page.
 */
public record DialogueRenderContext(
        DialogueDefinition dialogue,
        DialoguePage page,
        boolean fogEnabled,
        boolean characterBoxEnabled,
        boolean nameBoxEnabled,
        String characterName,
        String textColor,
        String nameColor,
        String infoColor,
        int contentOffsetPixels,
        int nameOffsetPixels
) {
    private static final String DEFAULT_TEXT_COLOR = "#f5edd7";
    private static final String DEFAULT_NAME_COLOR = "#f7d486";
    private static final String DEFAULT_INFO_COLOR = "#b8ad94";
    private static final int DEFAULT_CONTENT_OFFSET_PIXELS = -180;
    private static final int DEFAULT_NAME_OFFSET_PIXELS = -148;

    public DialogueRenderContext {
        textColor = blankToDefault(textColor, DEFAULT_TEXT_COLOR);
        nameColor = blankToDefault(nameColor, DEFAULT_NAME_COLOR);
        infoColor = blankToDefault(infoColor, DEFAULT_INFO_COLOR);
    }

    public static DialogueRenderContext of(DialogueDefinition dialogue, DialoguePage page) {
        Map<String, Object> settings = dialogue == null ? Map.of() : dialogue.settings().values();
        Map<String, Object> character = dialogue == null ? Map.of() : dialogue.settings().character();
        Map<String, Object> colors = dialogue == null ? Map.of() : dialogue.colors().values();
        Map<String, Object> offsets = dialogue == null ? Map.of() : dialogue.offsets().values();

        return new DialogueRenderContext(
                dialogue,
                page,
                booleanValue(settings, true, "fog", "fogEnabled", "showFog", "backgroundFog"),
                booleanValue(character, true, "enabled", "show", "showCharacter", "characterBox", "characterBoxEnabled"),
                booleanValue(settings, true, "nameBox", "nameBoxEnabled", "showName", "showNameBox"),
                stringValue(character, dialogue == null ? "" : dialogue.id(), "name", "displayName", "display-name", "Name"),
                stringValue(colors, DEFAULT_TEXT_COLOR, "text", "line", "lines", "dialogueText"),
                stringValue(colors, DEFAULT_NAME_COLOR, "name", "characterName", "speaker"),
                stringValue(colors, DEFAULT_INFO_COLOR, "info", "steadyInfo", "steadyInfoLine"),
                intValue(offsets, DEFAULT_CONTENT_OFFSET_PIXELS, "content", "contentOffset", "text", "x"),
                intValue(offsets, DEFAULT_NAME_OFFSET_PIXELS, "name", "nameOffset", "nameX")
        );
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static boolean booleanValue(Map<String, Object> values, boolean fallback, String... keys) {
        Object value = find(values, keys);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            return Boolean.parseBoolean(string);
        }
        return fallback;
    }

    private static int intValue(Map<String, Object> values, int fallback, String... keys) {
        Object value = find(values, keys);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String stringValue(Map<String, Object> values, String fallback, String... keys) {
        Object value = find(values, keys);
        return value == null ? fallback : String.valueOf(value);
    }

    private static Object find(Map<String, Object> values, String... keys) {
        if (values == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (values.containsKey(key)) {
                return values.get(key);
            }
            String normalized = normalize(key);
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (normalize(entry.getKey()).equals(normalized)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private static String normalize(String key) {
        return key == null ? "" : key.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
    }
}
