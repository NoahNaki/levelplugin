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
    private static final long ACTIVE_WINDOW_MS = 2_000L;
    private static final int MAX_INPUTS = 3;
    private static final char EMPTY_GLYPH = 'E';
    private static final char LEFT_GLYPH = 'L';
    private static final char RIGHT_GLYPH = 'R';

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
        if (state.castComplete || now - state.lastInputAt > COMBO_TIMEOUT_MS) {
            state.inputs.clear();
        }
        state.castComplete = false;
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
            return emptyGlyphs();
        }
        return formatGlyphs(state.inputs);
    }

    public String getComboSlot(Player player, int slotIndex) {
        if (player == null) {
            return "E";
        }
        if (slotIndex < 0 || slotIndex >= MAX_INPUTS) {
            return "E";
        }
        DisplayState state = states.get(player.getUniqueId());
        if (state == null || !isComboActive(player)) {
            return "E";
        }
        SpellClickInput input = getInputAt(state.inputs, slotIndex);
        if (input == null) {
            return "E";
        }
        return input == SpellClickInput.RIGHT ? "R" : "L";
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
            state.castComplete = false;
        }
    }

    public void markSpellCast(Player player) {
        if (player == null) {
            return;
        }
        DisplayState state = states.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        long now = System.currentTimeMillis();
        state.activeUntil = now + ACTIVE_WINDOW_MS;
        state.castComplete = true;
    }

    private String formatGlyphs(Deque<SpellClickInput> inputs) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (SpellClickInput input : inputs) {
            if (count >= MAX_INPUTS) {
                break;
            }
            sb.append(input == SpellClickInput.RIGHT ? RIGHT_GLYPH : LEFT_GLYPH);
            count++;
        }
        for (int i = count; i < MAX_INPUTS; i++) {
            sb.append(EMPTY_GLYPH);
        }
        return sb.toString();
    }

    private SpellClickInput getInputAt(Deque<SpellClickInput> inputs, int index) {
        int i = 0;
        for (SpellClickInput input : inputs) {
            if (i == index) {
                return input;
            }
            i++;
        }
        return null;
    }

    private String emptyGlyphs() {
        return String.valueOf(EMPTY_GLYPH).repeat(MAX_INPUTS);
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
        private boolean castComplete;
    }
}
