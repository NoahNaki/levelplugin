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
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpellInputListener implements Listener {
    private static final long COMBO_TIMEOUT_MS = 3_000L;
    private static final long SNEAK_WINDOW_MS = 700L;
    private static final long RIGHT_CLICK_DEBOUNCE_MS = 75L;

    private final SettingsManager settingsManager;
    private final Map<UUID, SpellComboTracker> comboTrackers = new HashMap<>();
    private final Map<UUID, SneakState> sneakStates = new HashMap<>();
    private final Map<UUID, Long> lastRightClickAt = new HashMap<>();
    private final SpellInputDisplayManager displayManager = SpellInputDisplayManager.getInstance();

    public SpellInputListener(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (isLeftClickAction(action)) {
            handleClick(player, true);
            return;
        }
        if (isRightClickAction(action)) {
            if (!shouldProcessRightClick(player)) {
                return;
            }
            handleClick(player, false);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!shouldProcessRightClick(event.getPlayer())) {
            return;
        }
        handleClick(event.getPlayer(), false);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }
        handleClick(player, true);
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
        lastRightClickAt.remove(playerId);
        displayManager.clear(event.getPlayer());
    }

    private void handleComboClick(Player player, boolean leftClick, boolean archerFamily) {
        SpellComboTracker tracker = comboTrackers.computeIfAbsent(player.getUniqueId(),
                id -> new SpellComboTracker(COMBO_TIMEOUT_MS));
        boolean comboStarted = tracker.hasInputs();
        boolean validComboStart = isComboStartClick(leftClick, archerFamily);
        if (!comboStarted && !validComboStart) {
            if (isBasicAttackClick(leftClick, archerFamily)) {
                dispatch(player, SpellInputType.BASIC_ATTACK, SpellInputMode.MOUSE_COMBO, leftClick ? "L" : "R");
            }
            return;
        }
        if (!comboStarted && isBasicAttackClick(leftClick, archerFamily)) {
            dispatch(player, SpellInputType.BASIC_ATTACK, SpellInputMode.MOUSE_COMBO, leftClick ? "L" : "R");
            return;
        }
        displayManager.recordClick(player, leftClick ? SpellClickInput.LEFT : SpellClickInput.RIGHT);
        SpellInputType result = tracker.recordClick(
                leftClick ? SpellClickInput.LEFT : SpellClickInput.RIGHT,
                archerFamily);
        if (result != null) {
            dispatch(player, result, SpellInputMode.MOUSE_COMBO, tracker.getLastSequence());
            displayManager.markSpellCast(player);
        }
    }

    private void handleModifierClick(Player player, boolean leftClick, boolean archerFamily) {
        displayManager.recordClick(player, leftClick ? SpellClickInput.LEFT : SpellClickInput.RIGHT);
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

    private boolean isComboStartClick(boolean leftClick, boolean archerFamily) {
        return archerFamily ? leftClick : !leftClick;
    }

    private boolean isArcherFamily(Player player) {
        PlayerClass playerClass = PlayerClassManager.getInstance().getPlayerClass(player);
        return ClassUtil.isArcherFamily(playerClass);
    }

    private boolean isLeftClickAction(Action action) {
        return action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
    }

    private boolean isRightClickAction(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    private void handleClick(Player player, boolean leftClick) {
        PlayerSettings settings = settingsManager.getSettings(player);
        SpellInputMode mode = settings.getSpellInputMode();
        boolean archerFamily = isArcherFamily(player);
        if (mode == SpellInputMode.MOUSE_COMBO) {
            handleComboClick(player, leftClick, archerFamily);
            return;
        }
        handleModifierClick(player, leftClick, archerFamily);
    }

    private void dispatch(Player player, SpellInputType type, SpellInputMode mode, String sequence) {
        Bukkit.getPluginManager().callEvent(new SpellInputEvent(player, type, mode, sequence));
        sendSpellCastIndicator(player, type);
    }

    private boolean shouldProcessRightClick(Player player) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        Long last = lastRightClickAt.get(playerId);
        if (last != null && now - last < RIGHT_CLICK_DEBOUNCE_MS) {
            return false;
        }
        lastRightClickAt.put(playerId, now);
        return true;
    }

    private void sendSpellCastIndicator(Player player, SpellInputType type) {
        if (type != SpellInputType.SPELL_1
                && type != SpellInputType.SPELL_2
                && type != SpellInputType.SPELL_3
                && type != SpellInputType.SPELL_4) {
            return;
        }
        PlayerClass playerClass = PlayerClassManager.getInstance().getPlayerClass(player);
        String className = playerClass != null ? playerClass.getDisplayName() : "Unknown";
        int spellNumber = type.ordinal();
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Spell " + spellNumber + " " + className + " Casted");
    }

    private static final class SneakState {
        private int count;
        private long lastReleaseAt;
    }
}
