package me.nakilex.levelplugin.spells.input;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import me.nakilex.levelplugin.utils.ChatMessageUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpellInputDisplayManager {
    private static final SpellInputDisplayManager instance = new SpellInputDisplayManager();

    private static final long COMBO_TIMEOUT_MS = 1_500L;
    private static final int MAX_INPUTS = 3;
    private static final String LEFT_GLYPH = "[papi:rf_lmb]";
    private static final String RIGHT_GLYPH = "[papi:rf_rmb]";
    private static final long DEBUG_THROTTLE_MS = 1_000L;

    public static SpellInputDisplayManager getInstance() {
        return instance;
    }

    private final Map<UUID, DisplayState> states = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDebugAt = new ConcurrentHashMap<>();

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
        if (state.inputs.size() == MAX_INPUTS) {
            state.inputs.removeFirst();
        }
        state.inputs.addLast(input);
    }

    public String getMouseComboDisplay(Player player) {
        return getMouseComboDisplay(player, false);
    }

    public String getMouseComboDisplay(Player player, boolean debug) {
        if (player == null) {
            return "";
        }
        DisplayState state = states.get(player.getUniqueId());
        if (state == null || state.inputs.isEmpty()) {
            if (debug) {
                sendDebug(player, "");
            }
            return "";
        }
        if (System.currentTimeMillis() - state.lastInputAt > COMBO_TIMEOUT_MS) {
            state.inputs.clear();
            if (debug) {
                sendDebug(player, "");
            }
            return "";
        }
        String display = formatInputs(state.inputs);
        if (debug) {
            sendDebug(player, display);
        }
        return display;
    }

    public String getComboSequence(Player player) {
        if (player == null) {
            return "";
        }
        DisplayState state = states.get(player.getUniqueId());
        if (state == null || state.inputs.isEmpty()) {
            return "";
        }
        if (System.currentTimeMillis() - state.lastInputAt > COMBO_TIMEOUT_MS) {
            state.inputs.clear();
            return "";
        }
        return formatSequence(state.inputs);
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        states.remove(player.getUniqueId());
        lastDebugAt.remove(player.getUniqueId());
    }

    private String formatInputs(Deque<SpellClickInput> inputs) {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (SpellClickInput input : inputs) {
            if (index == 1) {
                sb.append(ChatColor.GRAY).append(" - ");
            } else if (index == 2) {
                sb.append(ChatColor.GRAY).append(" ");
            }
            sb.append(input == SpellClickInput.RIGHT ? RIGHT_GLYPH : LEFT_GLYPH);
            index++;
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

    private void sendDebug(Player player, String display) {
        long now = System.currentTimeMillis();
        long last = lastDebugAt.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < DEBUG_THROTTLE_MS) {
            return;
        }
        lastDebugAt.put(player.getUniqueId(), now);
        String combo = getComboSequence(player);
        String text = display.isBlank() ? "empty" : display;
        String message = ChatColor.GRAY + "PAPI spell combo: " + ChatColor.WHITE + text
                + (combo.isBlank() ? "" : ChatColor.DARK_GRAY + " (" + combo + ")");
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, message);
    }

    private static final class DisplayState {
        private final Deque<SpellClickInput> inputs = new ArrayDeque<>(MAX_INPUTS);
        private long lastInputAt;
    }
}
