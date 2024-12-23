package me.nakilex.levelplugin.listeners;

import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.managers.SpellManager;
import me.nakilex.levelplugin.managers.StatsManager;
import me.nakilex.levelplugin.managers.StatsManager.PlayerStats;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ClickComboListener implements Listener {

    private static final long MAX_COMBO_TIME = 2000L; // 2 seconds
    private static final Map<UUID, ClickSequence> playerCombos = new HashMap<>();
    private final Map<UUID, Map<String, Long>> spellCooldowns = new HashMap<>();

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
        String className = ps.playerClass.name().toLowerCase();

        if (mainHand == null || mainHand.getType() == Material.AIR) {
            return;
        }

        if (className.equals("mage")) {
            String activeCombo = getActiveCombo(player);
            if (activeCombo.isEmpty()) {
                Spell basicSpell = new Spell(
                    "mage_basic",
                    "Basic Magic Attack",
                    "",
                    0, 0, 0,
                    List.of(Material.STICK),
                    "MAGE_BASIC",
                    1.0
                );
                basicSpell.castEffect(player);
                return;
            }
        }

        recordComboClick(player, "L");
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
        String className = ps.playerClass.name().toLowerCase();

        if (mainHand == null || mainHand.getType() == Material.AIR) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        recordComboClick(player, "R");
    }

    private void recordComboClick(Player player, String clickType) {
        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();

        ClickSequence seq = playerCombos.getOrDefault(uuid, new ClickSequence());
        if (now - seq.getLastClickTime() > MAX_COMBO_TIME) {
            seq.clear();
        }

        seq.addClick(clickType, now);
        playerCombos.put(uuid, seq);

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
