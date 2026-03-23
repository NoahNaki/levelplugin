package me.nakilex.levelplugin.spells.input;

import java.util.ArrayDeque;
import java.util.Deque;

public class SpellComboTracker {
    private final long comboTimeoutMs;
    private final Deque<SpellClickInput> inputs = new ArrayDeque<>(3);
    private long lastInputAt;
    private String lastSequence;

    public SpellComboTracker(long comboTimeoutMs) {
        this.comboTimeoutMs = comboTimeoutMs;
    }

    public String recordClick(SpellClickInput input, boolean archerFamily) {
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
        if (!SpellKeybindLayout.isValidComboSequence(archerFamily, sequence)) {
            return null;
        }
        return sequence;
    }

    public String getSequence() {
        StringBuilder sb = new StringBuilder(3);
        for (SpellClickInput input : inputs) {
            sb.append(input == SpellClickInput.RIGHT ? 'R' : 'L');
        }
        return sb.toString();
    }

    public String getLastSequence() {
        return lastSequence;
    }

    public boolean hasInputs() {
        long now = System.currentTimeMillis();
        if (now - lastInputAt > comboTimeoutMs) {
            inputs.clear();
            return false;
        }
        return !inputs.isEmpty();
    }

    public void reset() {
        inputs.clear();
        lastInputAt = 0L;
        lastSequence = null;
    }
}
