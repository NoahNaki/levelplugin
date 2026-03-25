package me.nakilex.levelplugin.spells.input;

import java.util.List;

public final class SpellKeybindLayout {
    private SpellKeybindLayout() {}

    private static final List<String> DEFAULT_COMBOS = List.of("RRL", "RLL", "RRR", "RLR");
    private static final List<String> ARCHER_COMBOS = List.of("LLR", "LRR", "LLL", "LRL");
    private static final List<String> KEYBOARD_INPUTS = List.of("Sneak+Right", "Sneak+Left", "Right", "Sneak+Sneak");
    private static final List<String> ARCHER_KEYBOARD_INPUTS = List.of("Sneak+Left", "Sneak+Right", "Left", "Sneak+Sneak");

    public static String comboSequenceForSlot(boolean archerFamily, SpellKeybindSlot slot) {
        List<String> combos = archerFamily ? ARCHER_COMBOS : DEFAULT_COMBOS;
        return combos.get(slot.ordinal());
    }

    public static SpellKeybindSlot comboSlotForSequence(boolean archerFamily, String sequence) {
        if (sequence == null) {
            return null;
        }
        List<String> combos = archerFamily ? ARCHER_COMBOS : DEFAULT_COMBOS;
        int idx = combos.indexOf(sequence.toUpperCase());
        if (idx < 0 || idx >= SpellKeybindSlot.values().length) {
            return null;
        }
        return SpellKeybindSlot.values()[idx];
    }

    public static boolean isValidComboSequence(boolean archerFamily, String sequence) {
        if (sequence == null) {
            return false;
        }
        List<String> combos = archerFamily ? ARCHER_COMBOS : DEFAULT_COMBOS;
        return combos.contains(sequence.toUpperCase());
    }

    public static String keyboardSequenceForSlot(boolean archerFamily, SpellKeybindSlot slot) {
        List<String> keys = archerFamily ? ARCHER_KEYBOARD_INPUTS : KEYBOARD_INPUTS;
        return keys.get(slot.ordinal());
    }

    public static String spellDisplayName(SpellInputType type) {
        if (type == null) {
            return "Unbound";
        }
        return switch (type) {
            case SPELL_1 -> "Spell 1";
            case SPELL_2 -> "Spell 2";
            case SPELL_3 -> "Spell 3";
            case SPELL_4 -> "Spell 4";
            default -> type.name();
        };
    }
}
