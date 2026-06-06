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
        int dialogueBackgroundOffsetPixels,
        int dialogueTextOffsetPixels,
        int characterOffsetPixels,
        int nameBackgroundOffsetPixels,
        int nameTextOffsetPixels,
        int infoTextOffsetPixels,
        int arrowOffsetPixels,
        int line1OffsetPixels,
        int line2OffsetPixels,
        int line3OffsetPixels,
        int line4OffsetPixels
) {
    public static final String TUNE_DIALOGUE_BACKGROUND_OFFSET = "dialogueBackgroundOffset";
    public static final String TUNE_DIALOGUE_TEXT_OFFSET = "dialogueTextOffset";
    public static final String TUNE_CHARACTER_OFFSET = "characterOffset";
    public static final String TUNE_NAME_BACKGROUND_OFFSET = "nameBackgroundOffset";
    public static final String TUNE_NAME_TEXT_OFFSET = "nameTextOffset";
    public static final String TUNE_INFO_TEXT_OFFSET = "infoTextOffset";
    public static final String TUNE_ARROW_OFFSET = "arrowOffset";
    public static final String TUNE_LINE_1_OFFSET = "line1Offset";
    public static final String TUNE_LINE_2_OFFSET = "line2Offset";
    public static final String TUNE_LINE_3_OFFSET = "line3Offset";
    public static final String TUNE_LINE_4_OFFSET = "line4Offset";

    private static final String DEFAULT_TEXT_COLOR = "#1e1e1e";
    private static final String DEFAULT_NAME_COLOR = "#f7d486";
    private static final String DEFAULT_INFO_COLOR = "#b8ad94";
    private static final int DEFAULT_DIALOGUE_BACKGROUND_OFFSET_PIXELS = -210;
    private static final int DEFAULT_DIALOGUE_TEXT_OFFSET_PIXELS = -165;
    private static final int DEFAULT_CHARACTER_OFFSET_PIXELS = -205;
    private static final int DEFAULT_NAME_BACKGROUND_OFFSET_PIXELS = -148;
    private static final int DEFAULT_NAME_TEXT_OFFSET_PIXELS = -140;
    private static final int DEFAULT_INFO_TEXT_OFFSET_PIXELS = -160;
    private static final int DEFAULT_ARROW_OFFSET_PIXELS = -188;

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

        int legacyContentOffset = intValue(offsets, DEFAULT_DIALOGUE_TEXT_OFFSET_PIXELS,
                "content", "contentOffset", "text", "x");
        int legacyNameOffset = intValue(offsets, DEFAULT_NAME_TEXT_OFFSET_PIXELS, "name", "nameOffset", "nameX");

        return new DialogueRenderContext(
                dialogue,
                page,
                booleanValue(settings, false, "fog", "fogEnabled", "showFog", "backgroundFog"),
                booleanValue(character, true, "enabled", "show", "showCharacter", "characterBox", "characterBoxEnabled"),
                booleanValue(settings, true, "nameBox", "nameBoxEnabled", "showName", "showNameBox"),
                stringValue(character, dialogue == null ? "" : dialogue.id(), "name", "displayName", "display-name", "Name"),
                stringValue(colors, DEFAULT_TEXT_COLOR, "text", "line", "lines", "dialogueText"),
                stringValue(colors, DEFAULT_NAME_COLOR, "name", "characterName", "speaker"),
                stringValue(colors, DEFAULT_INFO_COLOR, "info", "steadyInfo", "steadyInfoLine"),
                intValue(offsets, DEFAULT_DIALOGUE_BACKGROUND_OFFSET_PIXELS,
                        TUNE_DIALOGUE_BACKGROUND_OFFSET, "dialogue-background", "dialogueBackground"),
                intValue(offsets, legacyContentOffset, TUNE_DIALOGUE_TEXT_OFFSET, "dialogue-text", "dialogueText"),
                intValue(offsets, DEFAULT_CHARACTER_OFFSET_PIXELS, TUNE_CHARACTER_OFFSET, "character", "characterX"),
                intValue(offsets, DEFAULT_NAME_BACKGROUND_OFFSET_PIXELS,
                        TUNE_NAME_BACKGROUND_OFFSET, "name-background", "nameBackground"),
                intValue(offsets, legacyNameOffset, TUNE_NAME_TEXT_OFFSET, "name-text", "nameText"),
                intValue(offsets, DEFAULT_INFO_TEXT_OFFSET_PIXELS, TUNE_INFO_TEXT_OFFSET, "info-text", "infoText"),
                intValue(offsets, DEFAULT_ARROW_OFFSET_PIXELS, TUNE_ARROW_OFFSET, "arrow", "arrowX", "arrow-offset"),
                intValue(offsets, legacyContentOffset, TUNE_LINE_1_OFFSET, "line1", "line-1", "line1OffsetY"),
                intValue(offsets, legacyContentOffset, TUNE_LINE_2_OFFSET, "line2", "line-2", "line2OffsetY"),
                intValue(offsets, legacyContentOffset, TUNE_LINE_3_OFFSET, "line3", "line-3", "line3OffsetY"),
                intValue(offsets, legacyContentOffset, TUNE_LINE_4_OFFSET, "line4", "line-4", "line4OffsetY")
        );
    }

    public DialogueRenderContext withTuning(Map<String, Integer> tuning) {
        if (tuning == null || tuning.isEmpty()) {
            return this;
        }
        return new DialogueRenderContext(
                dialogue,
                page,
                fogEnabled,
                characterBoxEnabled,
                nameBoxEnabled,
                characterName,
                textColor,
                nameColor,
                infoColor,
                tuning.getOrDefault(TUNE_DIALOGUE_BACKGROUND_OFFSET, dialogueBackgroundOffsetPixels),
                tuning.getOrDefault(TUNE_DIALOGUE_TEXT_OFFSET, dialogueTextOffsetPixels),
                tuning.getOrDefault(TUNE_CHARACTER_OFFSET, characterOffsetPixels),
                tuning.getOrDefault(TUNE_NAME_BACKGROUND_OFFSET, nameBackgroundOffsetPixels),
                tuning.getOrDefault(TUNE_NAME_TEXT_OFFSET, nameTextOffsetPixels),
                tuning.getOrDefault(TUNE_INFO_TEXT_OFFSET, infoTextOffsetPixels),
                tuning.getOrDefault(TUNE_ARROW_OFFSET, arrowOffsetPixels),
                tuning.getOrDefault(TUNE_LINE_1_OFFSET, line1OffsetPixels),
                tuning.getOrDefault(TUNE_LINE_2_OFFSET, line2OffsetPixels),
                tuning.getOrDefault(TUNE_LINE_3_OFFSET, line3OffsetPixels),
                tuning.getOrDefault(TUNE_LINE_4_OFFSET, line4OffsetPixels)
        );
    }

    public int contentOffsetPixels() {
        return dialogueTextOffsetPixels;
    }

    public int nameOffsetPixels() {
        return nameTextOffsetPixels;
    }

    public int lineOffsetPixels(int lineNumber) {
        return switch (lineNumber) {
            case 1 -> line1OffsetPixels;
            case 2 -> line2OffsetPixels;
            case 3 -> line3OffsetPixels;
            case 4 -> line4OffsetPixels;
            default -> dialogueTextOffsetPixels;
        };
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
