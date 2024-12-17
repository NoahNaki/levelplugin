package me.nakilex.levelplugin.listeners;

import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.SpellManager;
import me.nakilex.levelplugin.managers.StatsManager;
import me.nakilex.levelplugin.managers.StatsManager.PlayerStats;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

public class ClickComboListener implements Listener {

    private static final long MAX_COMBO_TIME = 2000L; // 2 seconds
    private final Map<UUID, ClickSequence> playerCombos = new HashMap<>();
    private final Map<UUID, Map<String, Long>> spellCooldowns = new HashMap<>();

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        if (mainHand == null || mainHand.getType() == Material.AIR) {
            return;
        }

        Action action = event.getAction();
        String clickType = null;
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            clickType = "R";
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            clickType = "L";
        } else {
            return;
        }

        PlayerStats pStats = StatsManager.getInstance().getPlayerStats(player);
        String className = pStats.playerClass.name().toLowerCase();

        // Archer auto-attack logic if R-click with bow/crossbow
        if (className.equals("archer") && isBowOrCrossbow(mainHand.getType()) && clickType.equals("R")) {
            ClickSequence seq = playerCombos.get(player.getUniqueId());
            boolean partialCombo = (seq != null && seq.getComboString().length() > 0);

            if (!partialCombo) {
                // Fire an arrow instantly with speed=3.0
                event.setCancelled(true);
                spawnCustomArrow(player, 3.0f, false, player.getLocation().getDirection());
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1f);
                return; // skip combos
            }
            // if partial combo is in progress, continue below
        }

        recordComboClick(player, clickType);
    }

    private boolean isBowOrCrossbow(Material mat) {
        return (mat == Material.BOW || mat == Material.CROSSBOW);
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

        // show partial combo in action bar
        showActionBarCombo(player, seq.getComboString());

        if (seq.isComplete()) {
            String combo = seq.getComboString();
            seq.clear();
            handleSpellCast(player, combo);
        }
    }

    private void showActionBarCombo(Player player, String partialCombo) {
        PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
        double hp = player.getHealth();
        double maxHp = player.getMaxHealth();
        int currentMana = (int) ps.currentMana;
        int maxMana = ps.maxMana;

        String message = "§c" + (int)hp + "/" + (int)maxHp
            + "  §e[" + partialCombo + "]  "
            + "§b" + currentMana + "/" + maxMana;
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    private void handleSpellCast(Player player, String combo) {
        PlayerStats ps = StatsManager.getInstance().getPlayerStats(player);
        String className = ps.playerClass.name().toLowerCase();

        Spell spell = SpellManager.getInstance().getSpell(className, combo);
        if (spell == null) {
            player.sendMessage("§cInvalid combo: " + combo);
            return;
        }

        // If Archer but combo doesn't start with L => invalid
        if (className.equals("archer") && !combo.startsWith("L")) {
            player.sendMessage("§cArcher combos must start with L!");
            return;
        } else if (!className.equals("archer") && !combo.startsWith("R")) {
            player.sendMessage("§cThis class combos must start with R!");
            return;
        }

        // Check weapon
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (!spell.getAllowedWeapons().contains(mainHand.getType())) {
            player.sendMessage("§cYou must hold a valid " + className + " weapon to cast spells!");
            return;
        }

        // Check level
        int playerLevel = StatsManager.getInstance().getLevel(player);
        if (playerLevel < spell.getLevelReq()) {
            player.sendMessage("§cYou are not high enough level to cast " + spell.getDisplayName());
            return;
        }

        // Check mana
        if (ps.currentMana < spell.getManaCost()) {
            player.sendMessage("§cNot enough mana to cast " + spell.getDisplayName());
            return;
        }

        // Check cooldown
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

        // If Archer, do our custom archer logic
        if (className.equals("archer")) {
            castArcherSpell(player, spell);
        } else {
            // For other classes, fallback to generic logic or spell.castEffect(...)
            spell.castEffect(player);
        }

        // Deduct mana
        ps.currentMana -= spell.getManaCost();
        StatsManager.getInstance().recalcDerivedStats(player);

        // set cooldown
        long nextUse = now + (spell.getCooldown() * 1000L);
        cdMap.put(spell.getId(), nextUse);
        spellCooldowns.put(player.getUniqueId(), cdMap);
    }

    /**
     * Specifically handle Archer combos to ensure each skill does the intended effect.
     */
    private void castArcherSpell(Player player, Spell spell) {
        String effectKey = spell.getEffectKey().toUpperCase();

        switch(effectKey) {
            case "POWER_SHOT":
                // single arrow, double damage, piercing
                spawnCustomArrow(player, 3.0f, true, player.getLocation().getDirection());
                player.sendMessage("§ePower Shot! A piercing arrow dealing 200% weapon damage!");
                break;

            case "ARROW_STORM":
                // 10 arrows in a cone, each 50% damage
                player.sendMessage("§eArrow Storm! Firing 10 arrows in a cone!");
                double angleInc = Math.toRadians(10);
                Vector forward = player.getLocation().getDirection().normalize();
                for (int i = 0; i < 10; i++) {
                    double angle = (i - 5) * angleInc;
                    Vector spread = rotateVector(forward.clone(), angle);
                    spawnCustomArrow(player, 2.0f, false, spread);
                }
                break;

            case "EXPLOSIVE_ARROW":
                // single arrow that explodes on impact
                Arrow explosive = spawnCustomArrow(player, 3.0f, false, player.getLocation().getDirection());
                explosive.setCustomName("EXPLOSIVE_ARROW");
                explosive.setCritical(true);
                player.sendMessage("§eExplosive Arrow fired! (AoE on impact)");
                break;

            case "SWIFT_ESCAPE_HOOK":
            case "GRAPPLE_HOOK":
            case "LRR": // If the effectKey is actually "LRR"
                // propel user backward
                player.sendMessage("§eSwift Escape Hook! You leap backwards quickly!");
                Vector backward = player.getLocation().getDirection().multiply(-2);
                player.setVelocity(backward);
                break;

            default:
                // fallback or call spell.castEffect
                spell.castEffect(player);
                break;
        }
    }

    /**
     * Spawns a custom arrow from the player's eye location with a given speed, direction,
     * and optional piercing.
     */
    private Arrow spawnCustomArrow(Player player, float speed, boolean piercing, Vector direction) {
        Location loc = player.getEyeLocation();
        Arrow arrow = player.getWorld().spawnArrow(loc, direction, speed, 0f);
        arrow.setShooter(player);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        if (piercing) {
            arrow.setPierceLevel((byte)1);
        }
        return arrow;
    }

    private static Vector rotateVector(Vector v, double yawRadians) {
        double cos = Math.cos(yawRadians);
        double sin = Math.sin(yawRadians);
        double x = v.getX() * cos - v.getZ() * sin;
        double z = v.getX() * sin + v.getZ() * cos;
        return new Vector(x, v.getY(), z);
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
}
