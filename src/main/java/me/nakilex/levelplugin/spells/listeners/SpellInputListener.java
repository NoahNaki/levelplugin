package me.nakilex.levelplugin.spells.listeners;

import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.settings.data.PlayerSettings;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.spells.SpellAccessUtil;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.input.SpellComboTracker;
import me.nakilex.levelplugin.spells.input.SpellClickInput;
import me.nakilex.levelplugin.spells.input.SpellInputDisplayManager;
import me.nakilex.levelplugin.spells.input.SpellInputEvent;
import me.nakilex.levelplugin.spells.input.SpellInputMode;
import me.nakilex.levelplugin.spells.input.SpellInputType;
import me.nakilex.levelplugin.spells.input.SpellKeybindLayout;
import me.nakilex.levelplugin.spells.input.SpellKeybindManager;
import me.nakilex.levelplugin.spells.input.SpellKeybindSlot;
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
import org.bukkit.inventory.ItemStack;

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
    private final SpellKeybindManager keybindManager = SpellKeybindManager.getInstance();

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
        if (player.hasMetadata(SpellEffectUtil.BYPASS_STAT_SCALING_META)) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }
        PlayerClass playerClass = PlayerClassManager.getInstance().getPlayerClass(player);
        if (ClassUtil.isRogueFamily(playerClass)) {
            event.setCancelled(true);
        }
        handleClick(player, true);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            return;
        }
        Player player = event.getPlayer();
        if (!isHoldingValidClassWeapon(player)) {
            return;
        }
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
            dispatchBoundSpell(player, SpellInputMode.MOUSE_AND_KEYBOARD, SpellKeybindSlot.SLOT_3, "Sneak+Sneak");
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
        if (isMainHandEmpty(player)) {
            return;
        }
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
        if (!comboStarted && validComboStart) {
            displayManager.clearInputs(player);
        }
        displayManager.recordClick(player, leftClick ? SpellClickInput.LEFT : SpellClickInput.RIGHT);
        String sequence = tracker.recordClick(
                leftClick ? SpellClickInput.LEFT : SpellClickInput.RIGHT,
                archerFamily);
        if (sequence != null) {
            SpellInputType bound = getBoundSpell(player, SpellInputMode.MOUSE_COMBO,
                    SpellKeybindLayout.comboSlotForSequence(archerFamily, sequence));
            if (bound != null) {
                dispatch(player, bound, SpellInputMode.MOUSE_COMBO, tracker.getLastSequence());
            }
        }
    }

    private SpellInputType getBoundSpell(Player player, SpellInputMode mode, SpellKeybindSlot slot) {
        if (player == null || mode == null || slot == null) {
            return null;
        }
        PlayerClass playerClass = PlayerClassManager.getInstance().getPlayerClass(player);
        return keybindManager.getBinding(player.getUniqueId(), playerClass, mode, slot);
    }

    private void handleModifierClick(Player player, boolean leftClick, boolean archerFamily) {
        displayManager.recordClick(player, leftClick ? SpellClickInput.LEFT : SpellClickInput.RIGHT);
        if (player.isSneaking()) {
            SpellKeybindSlot slot = leftClick ? SpellKeybindSlot.SLOT_1 : SpellKeybindSlot.SLOT_2;
            dispatchBoundSpell(player, SpellInputMode.MOUSE_AND_KEYBOARD, slot,
                    leftClick ? "Sneak+Left" : "Sneak+Right");
            return;
        }
        if (isBasicAttackClick(leftClick, archerFamily)) {
            dispatch(player, SpellInputType.BASIC_ATTACK, SpellInputMode.MOUSE_AND_KEYBOARD,
                    leftClick ? "Left" : "Right");
        } else if (!leftClick) {
            dispatchBoundSpell(player, SpellInputMode.MOUSE_AND_KEYBOARD, SpellKeybindSlot.SLOT_4, "Right");
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

    private boolean isMainHandEmpty(Player player) {
        return player.getInventory().getItemInMainHand().getType().isAir();
    }

    private boolean isLeftClickAction(Action action) {
        return action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
    }

    private boolean isRightClickAction(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    private void handleClick(Player player, boolean leftClick) {
        boolean validWeapon = isHoldingValidClassWeapon(player);
        if (!validWeapon) {
            if (!isMageBasicFallbackAllowed(player, leftClick)) {
                return;
            }
            dispatch(player, SpellInputType.BASIC_ATTACK, SpellInputMode.MOUSE_AND_KEYBOARD,
                    leftClick ? "Left" : "Right");
            return;
        }
        sendClickDebug(player, leftClick);
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

    private void dispatchBoundSpell(Player player, SpellInputMode mode, SpellKeybindSlot slot, String sequence) {
        SpellInputType bound = getBoundSpell(player, mode, slot);
        if (bound != null) {
            dispatch(player, bound, mode, sequence);
        }
    }

    private void sendClickDebug(Player player, boolean leftClick) {
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Click " + (leftClick ? "Left" : "Right") + " Class: " + getPlayerClassName(player));
    }

    private boolean isMageBasicFallbackAllowed(Player player, boolean leftClick) {
        if (player == null || !leftClick) {
            return false;
        }
        if (player.getInventory().getItemInMainHand().getType().isAir()) {
            return false;
        }
        PlayerClass playerClass = PlayerClassManager.getInstance().getPlayerClass(player);
        return ClassUtil.isMageFamily(playerClass);
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
        int spellNumber = type.ordinal();
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Spell " + spellNumber + " " + getPlayerClassName(player) + " Casted");
    }

    private String getPlayerClassName(Player player) {
        PlayerClass playerClass = PlayerClassManager.getInstance().getPlayerClass(player);
        return playerClass != null ? playerClass.getDisplayName() : "Unknown";
    }

    private boolean isHoldingValidClassWeapon(Player player) {
        return SpellAccessUtil.isHoldingValidClassWeapon(player);
    }

    private static final class SneakState {
        private int count;
        private long lastReleaseAt;
    }
}
