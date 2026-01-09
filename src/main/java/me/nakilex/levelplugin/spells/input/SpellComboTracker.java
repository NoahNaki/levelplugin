package me.nakilex.levelplugin.spells.input;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class SpellComboTracker {
    private static final List<String> DEFAULT_COMBOS = List.of("RRL", "RLR", "RRR", "RLL");
    private static final List<String> ARCHER_COMBOS = List.of("LLR", "LLL", "LRL", "LRR");

    private final long comboTimeoutMs;
    private final Deque<ClickInput> inputs = new ArrayDeque<>(3);
    private long lastInputAt;
    private String lastSequence;

    public SpellComboTracker(long comboTimeoutMs) {
        this.comboTimeoutMs = comboTimeoutMs;
    }

    public SpellInputType recordClick(ClickInput input, boolean archerFamily) {
        long now = System.currentTimeMillis();
        if (now - lastInputAt > comboTimeoutMs) {
            inputs.clear();
        }
        lastInputAt = now;
        if (inputs.size() == 3) {
            inputs.removeFirst();
        }
        inputs.addLast(input);
        if (inputs.size() < 3) {
            return null;
        }
        String sequence = getSequence();
        lastSequence = sequence;
        inputs.clear();
        List<String> combos = archerFamily ? ARCHER_COMBOS : DEFAULT_COMBOS;
        int idx = combos.indexOf(sequence);
        if (idx == -1) {
            return null;
        }
        return SpellInputType.values()[SpellInputType.SPELL_1.ordinal() + idx];
    }

    public String getSequence() {
        StringBuilder sb = new StringBuilder(3);
        for (ClickInput input : inputs) {
            sb.append(input == ClickInput.RIGHT ? 'R' : 'L');
        }
        return sb.toString();
    }

    public String getLastSequence() {
        return lastSequence;
    }

    public enum ClickInput {
        LEFT,
        RIGHT
    }
}
