package me.nakilex.levelplugin.spells.listeners;

import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.settings.data.PlayerSettings;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.spells.input.SpellComboTracker;
import me.nakilex.levelplugin.spells.input.SpellClickInput;
import me.nakilex.levelplugin.spells.input.SpellInputDisplayManager;
import me.nakilex.levelplugin.spells.input.SpellInputEvent;
import me.nakilex.levelplugin.spells.input.SpellInputMode;
import me.nakilex.levelplugin.spells.input.SpellInputType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.block.Action;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpellInputListener implements Listener {
    private static final long COMBO_TIMEOUT_MS = 3_000L;
    private static final long SNEAK_WINDOW_MS = 700L;

    private final SettingsManager settingsManager;
    private final Map<UUID, SpellComboTracker> comboTrackers = new HashMap<>();
    private final Map<UUID, SneakState> sneakStates = new HashMap<>();
    private final SpellInputDisplayManager displayManager = SpellInputDisplayManager.getInstance();

    public SpellInputListener(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (!isClickAction(action)) {
            return;
        }
        Player player = event.getPlayer();
        PlayerSettings settings = settingsManager.getSettings(player);
        SpellInputMode mode = settings.getSpellInputMode();
        boolean archerFamily = isArcherFamily(player);
        boolean leftClick = isLeftClick(action);
        if (mode == SpellInputMode.MOUSE_COMBO) {
            handleComboClick(player, leftClick, archerFamily);
            return;
        }
        handleModifierClick(player, leftClick, archerFamily);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            return;
        }
        Player player = event.getPlayer();
        PlayerSettings settings = settingsManager.getSettings(player);
        if (settings.getSpellInputMode() != SpellInputMode.MOUSE_AND_KEYBOARD) {
            return;
        }
        SneakState state = sneakStates.computeIfAbsent(player.getUniqueId(), id -> new SneakState());
        long now = System.currentTimeMillis();
        if (now - state.lastReleaseAt > SNEAK_WINDOW_MS) {
            state.count = 0;
        }
        state.count++;
        state.lastReleaseAt = now;
        if (state.count >= 2) {
            state.count = 0;
            dispatch(player, SpellInputType.SPELL_3, SpellInputMode.MOUSE_AND_KEYBOARD, "Sneak+Sneak");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        comboTrackers.remove(playerId);
        sneakStates.remove(playerId);
        displayManager.clear(event.getPlayer());
    }

    private void handleComboClick(Player player, boolean leftClick, boolean archerFamily) {
        if (isBasicAttackClick(leftClick, archerFamily)) {
            dispatch(player, SpellInputType.BASIC_ATTACK, SpellInputMode.MOUSE_COMBO, leftClick ? "L" : "R");
        }
        displayManager.recordClick(player, leftClick ? SpellClickInput.LEFT : SpellClickInput.RIGHT);
        SpellComboTracker tracker = comboTrackers.computeIfAbsent(player.getUniqueId(),
                id -> new SpellComboTracker(COMBO_TIMEOUT_MS));
        SpellInputType result = tracker.recordClick(
                leftClick ? SpellClickInput.LEFT : SpellClickInput.RIGHT,
                archerFamily);
        if (result != null) {
            dispatch(player, result, SpellInputMode.MOUSE_COMBO, tracker.getLastSequence());
            displayManager.clearInputs(player);
        }
    }

    private void handleModifierClick(Player player, boolean leftClick, boolean archerFamily) {
        if (player.isSneaking()) {
            SpellInputType type = leftClick ? SpellInputType.SPELL_1 : SpellInputType.SPELL_2;
            dispatch(player, type, SpellInputMode.MOUSE_AND_KEYBOARD,
                    leftClick ? "Sneak+Left" : "Sneak+Right");
            return;
        }
        if (isBasicAttackClick(leftClick, archerFamily)) {
            dispatch(player, SpellInputType.BASIC_ATTACK, SpellInputMode.MOUSE_AND_KEYBOARD,
                    leftClick ? "Left" : "Right");
        } else if (!leftClick) {
            dispatch(player, SpellInputType.SPELL_4, SpellInputMode.MOUSE_AND_KEYBOARD, "Right");
        }
    }

    private boolean isBasicAttackClick(boolean leftClick, boolean archerFamily) {
        return archerFamily ? !leftClick : leftClick;
    }

    private boolean isArcherFamily(Player player) {
        PlayerClass playerClass = PlayerClassManager.getInstance().getPlayerClass(player);
        return ClassUtil.isArcherFamily(playerClass);
    }

    private boolean isClickAction(Action action) {
        return action == Action.LEFT_CLICK_AIR
                || action == Action.LEFT_CLICK_BLOCK
                || action == Action.RIGHT_CLICK_AIR
                || action == Action.RIGHT_CLICK_BLOCK;
    }

    private boolean isLeftClick(Action action) {
        return action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
    }

    private void dispatch(Player player, SpellInputType type, SpellInputMode mode, String sequence) {
        Bukkit.getPluginManager().callEvent(new SpellInputEvent(player, type, mode, sequence));
    }

    private static final class SneakState {
        private int count;
        private long lastReleaseAt;
    }
}
