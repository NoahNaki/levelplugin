package me.nakilex.levelplugin.spells.input;

import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpellInputDisplayManager {
    private static final SpellInputDisplayManager instance = new SpellInputDisplayManager();

    private static final long COMBO_TIMEOUT_MS = 900L;
    private static final long ACTIVE_WINDOW_MS = 1_200L;
    private static final int MAX_INPUTS = 3;
    private static final char LEFT_GLYPH = '\uE001';
    private static final char RIGHT_GLYPH = '\uE002';

    public static SpellInputDisplayManager getInstance() {
        return instance;
    }

    private final Map<UUID, DisplayState> states = new ConcurrentHashMap<>();
    public void recordClick(Player player, SpellClickInput input) {
        if (player == null || input == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        DisplayState state = states.computeIfAbsent(playerId, id -> new DisplayState());
        long now = System.currentTimeMillis();
        if (now - state.lastInputAt > COMBO_TIMEOUT_MS) {
            state.inputs.clear();
        }
        state.lastInputAt = now;
        state.activeUntil = now + ACTIVE_WINDOW_MS;
        if (state.inputs.size() == MAX_INPUTS) {
            state.inputs.removeFirst();
        }
        state.inputs.addLast(input);
    }

    public boolean isComboActive(Player player) {
        if (player == null) {
            return false;
        }
        DisplayState state = states.get(player.getUniqueId());
        return state != null && System.currentTimeMillis() < state.activeUntil;
    }

    public String getComboGlyphs(Player player) {
        if (player == null) {
            return "";
        }
        DisplayState state = states.get(player.getUniqueId());
        if (state == null || !isComboActive(player) || state.inputs.isEmpty()) {
            return "";
        }
        return formatGlyphs(state.inputs);
    }

    public String getComboSequence(Player player) {
        if (player == null) {
            return "";
        }
        DisplayState state = states.get(player.getUniqueId());
        if (state == null || state.inputs.isEmpty() || !isComboActive(player)) {
            return "";
        }
        return formatSequence(state.inputs);
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        states.remove(player.getUniqueId());
    }

    public void clearInputs(Player player) {
        if (player == null) {
            return;
        }
        DisplayState state = states.get(player.getUniqueId());
        if (state != null) {
            state.inputs.clear();
        }
    }

    private String formatGlyphs(Deque<SpellClickInput> inputs) {
        StringBuilder sb = new StringBuilder();
        for (SpellClickInput input : inputs) {
            sb.append(input == SpellClickInput.RIGHT ? RIGHT_GLYPH : LEFT_GLYPH);
        }
        return sb.toString();
    }

    private String formatSequence(Deque<SpellClickInput> inputs) {
        StringBuilder sb = new StringBuilder();
        for (SpellClickInput input : inputs) {
            sb.append(input == SpellClickInput.RIGHT ? 'R' : 'L');
        }
        return sb.toString();
    }

    private static final class DisplayState {
        private final Deque<SpellClickInput> inputs = new ArrayDeque<>(MAX_INPUTS);
        private long lastInputAt;
        private long activeUntil;
    }
}
