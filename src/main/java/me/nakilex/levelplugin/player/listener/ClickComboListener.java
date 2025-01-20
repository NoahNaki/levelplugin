package me.nakilex.levelplugin.player.listener;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.PlayerStats;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ClickComboListener implements Listener {

    private static final long MAX_COMBO_TIME = 2000L; // 2 seconds
    private static final Map<UUID, ClickSequence> playerCombos = new HashMap<>();
    private final Map<UUID, Map<String, Long>> spellCooldowns = new HashMap<>();
    private final Map<UUID, Long> activeLeftClicks = new HashMap<>();



    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Check for recent clicks
        if (activeLeftClicks.containsKey(playerId)) {
            long lastClickTime = activeLeftClicks.get(playerId);
            if (currentTime - lastClickTime < 100) { // Adjust the threshold if necessary
                return; // Ignore duplicate left-click events
            }
        }

        // Register the click
        activeLeftClicks.put(playerId, currentTime);

        // Schedule removal after a short delay
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> activeLeftClicks.remove(playerId), 5L);

        // Process the left-click
        recordComboClick(player, "L");
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        // Ensure the event is triggered only for main hand and right-click actions
        if (event.getHand() != EquipmentSlot.HAND ||
            (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;

        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
        String className = ps.playerClass.name().toLowerCase();

        // Verify main hand is not empty
        if (mainHand == null || mainHand.getType() == Material.AIR) {
            return;
        }

        // Record the right-click for combos
        recordComboClick(player, "R");
    }

    private void recordComboClick(Player player, String clickType) {
        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();

        // Retrieve the player's stats and class
        PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
        String className = ps.playerClass.name().toLowerCase();

        // Check if the player is holding a valid weapon for their class
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand == null || mainHand.getType() == Material.AIR) {
            return; // Don't update action bar if no weapon is held
        }

        // Retrieve all spells for the player's class
        Map<String, Spell> classSpells = SpellManager.getInstance().getSpellsByClass(className);
        if (classSpells == null || classSpells.isEmpty()) {
            return; // No spells found for the player's class
        }

        // Check if the weapon is valid for any spell in the player's class
        boolean validWeapon = classSpells.values().stream()
            .anyMatch(spell -> spell.getAllowedWeapons().contains(mainHand.getType()));

        if (!validWeapon) {
            return; // Don't update action bar if the weapon is invalid
        }

        // Proceed with combo recording
        ClickSequence seq = playerCombos.getOrDefault(uuid, new ClickSequence());
        if (now - seq.getLastClickTime() > MAX_COMBO_TIME) {
            seq.clear();
        }

        seq.addClick(clickType, now);
        playerCombos.put(uuid, seq);

        // If the combo is complete, handle the spell cast
        if (seq.isComplete()) {
            String combo = seq.getComboString();
            seq.clear();
            handleSpellCast(player, combo);
        }
    }

    private void handleSpellCast(Player player, String combo) {
        PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
        String className = ps.playerClass.name().toLowerCase();

        Spell spell = SpellManager.getInstance().getSpell(className, combo);
        if (spell == null) {
            return;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (!spell.getAllowedWeapons().contains(mainHand.getType())) {
            player.sendMessage("§cYou must hold a valid " + className + " weapon to cast spells!");
            return;
        }

        int playerLevel = StatsManager.getInstance().getLevel(player);
        if (playerLevel < spell.getLevelReq()) {
            player.sendMessage("§cYou are not high enough level to cast " + spell.getDisplayName());
            return;
        }

        if (ps.currentMana < spell.getManaCost()) {
            player.sendMessage("§cNot enough mana to cast " + spell.getDisplayName());
            return;
        }

        long now = System.currentTimeMillis();
        Map<String, Long> cdMap = spellCooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        if (cdMap.containsKey(spell.getId())) {
            long nextUsable = cdMap.get(spell.getId());
            if (now < nextUsable) {
                long secsLeft = (nextUsable - now) / 1000;
                player.sendMessage("§cSpell on cooldown for " + secsLeft + "s");
                return;
            }
        }

        spell.castEffect(player);

        ps.currentMana -= spell.getManaCost();
        StatsManager.getInstance().recalcDerivedStats(player);

        long nextUse = now + (spell.getCooldown() * 1000L);
        cdMap.put(spell.getId(), nextUse);
        spellCooldowns.put(player.getUniqueId(), cdMap);
    }

    public static String getActiveCombo(Player player) {
        UUID uuid = player.getUniqueId();
        ClickSequence seq = playerCombos.get(uuid);
        return (seq != null) ? seq.getComboString() : "";
    }

    private static class ClickSequence {
        private StringBuilder clicks = new StringBuilder();
        private long lastClickTime = 0L;

        void addClick(String c, long time) {
            clicks.append(c);
            lastClickTime = time;
        }

        boolean isComplete() {
            return clicks.length() >= 3;
        }

        String getComboString() {
            return clicks.toString();
        }

        long getLastClickTime() {
            return lastClickTime;
        }

        void clear() {
            clicks.setLength(0);
            lastClickTime = 0L;
        }
    }

    public static boolean isLocTpSafe(Location location) {
        Block block = location.getBlock();
        return !block.isLiquid() && block.isPassable();
    }
}
