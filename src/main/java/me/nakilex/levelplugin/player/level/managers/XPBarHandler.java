package me.nakilex.levelplugin.player.level.managers;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;
import java.util.UUID;

public class XPBarHandler {

    /**
     * Update the player's XP bar and level display.
     */
    public static void updateXPBar(Player player, LevelManager levelManager) {
        int currentLevel = levelManager.getLevel(player);
        int currentXP    = levelManager.getXP(player);
        int xpNeeded     = levelManager.getXpNeededForNextLevel(player);

        if (currentLevel >= levelManager.getMaxLevel()) {
            player.setLevel(currentLevel);
            player.setExp(1.0f);
            return;
        }

        player.setLevel(currentLevel);
        if (xpNeeded > 0) {
            float progress = (float) currentXP / (float) xpNeeded;
            player.setExp(Math.min(progress, 0.999f));
        } else {
            player.setExp(0.0f);
        }
    }

    /**
     * Handle visual and stat updates when a player levels up.
     */
    public static void handleLevelUpEvent(Player player, int newLevel, int xpNeeded) {
        // DIVIDER & MESSAGES
        ChatFormatter.constructDivider(player, "§a§l-", 45);
        ChatFormatter.sendCenteredMessage(player, "§6§lLEVEL UP!");
        ChatFormatter.sendCenteredMessage(player, "");
        ChatFormatter.sendCenteredMessage(player, "§7You are now level §e§l" + newLevel + "§7!");
        ChatFormatter.sendCenteredMessage(player, "§7You need §e" + xpNeeded + " xp §7to reach level §e" + (newLevel + 1) + "§7.");
        ChatFormatter.sendCenteredMessage(player, "");
        ChatFormatter.sendCenteredMessage(player, "§a+3 §7Stat Points have been added");
        ChatFormatter.constructDivider(player, "§a§l-", 45);

        // SOUND & PARTICLES
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 30);
        launchFirework(player.getLocation());

        // UPDATE CUSTOM ITEM TOOLTIPS
        player.getInventory().forEach(stack -> {
            if (stack != null && stack.hasItemMeta()
                && stack.getItemMeta().getPersistentDataContainer().has(ItemUtil.ITEM_UUID_KEY, PersistentDataType.STRING)) {
                ItemUtil.updateCustomItemTooltip(stack, player);
            }
        });
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.hasItemMeta()
                && armor.getItemMeta().getPersistentDataContainer().has(ItemUtil.ITEM_UUID_KEY, PersistentDataType.STRING)) {
                ItemUtil.updateCustomItemTooltip(armor, player);
            }
        }
        player.updateInventory();

        // AUTO-APPLY WEAPON STATS ON LEVEL-UP
        UUID puuid = player.getUniqueId();
        StatsManager statsMgr = StatsManager.getInstance();
        Set<Integer> equipped = statsMgr.getEquippedItems(puuid);

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon != null && !weapon.getType().isAir()
            && WeaponType.matchType(weapon) != null) {

            CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(weapon);
            if (ci != null) {
                int id           = ci.getId();
                int reqLevel     = ci.getLevelRequirement();
                String clsReqRaw = ci.getClassRequirement();

                PlayerClass reqClass;
                try {
                    reqClass = PlayerClass.valueOf(clsReqRaw.toUpperCase());
                } catch (IllegalArgumentException e) {
                    reqClass = PlayerClass.VILLAGER;
                }
                PlayerClass playerClass = statsMgr.getPlayerStats(puuid).playerClass;

                // Check both level AND class requirements
                if (!equipped.contains(id)
                    && newLevel >= reqLevel
                    && (reqClass == PlayerClass.VILLAGER || reqClass == playerClass)) {

                    // ADD STATS
                    StatsManager.PlayerStats ps = statsMgr.getPlayerStats(puuid);
                    ps.bonusHealthStat   += ci.getHp();
                    ps.bonusDefenceStat  += ci.getDef();
                    ps.bonusStrength     += ci.getStr();
                    ps.bonusAgility      += ci.getAgi();
                    ps.bonusIntelligence += ci.getIntel();
                    ps.bonusDexterity    += ci.getDex();
                    equipped.add(id);

                    player.sendMessage(ChatColor.GREEN
                        + "Your " + ci.getBaseName()
                        + " now grants its stats, since you reached level "
                        + newLevel + " and meet the class requirement!");
                }
            }
        }

        // RECALCULATE DERIVED STATS
        statsMgr.recalcDerivedStats(player);

        // ─── FULL HEAL & MANA REFILL ────────────────────────────────────────────
        // Heal the player to their new max health
        player.setHealth(player.getMaxHealth());
        // Refill their mana pool to max
        StatsManager.PlayerStats ps = StatsManager
            .getInstance()
            .getPlayerStats(player.getUniqueId());
        ps.setCurrentMana(ps.getMaxMana());
        // ────────────────────────────────────────────────────────────────────────
    }


    /**
     * Launches a decorative firework at the given location.
     */
    public static void launchFirework(Location location) {
        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = firework.getFireworkMeta();

        FireworkEffect effect = FireworkEffect.builder()
            .withColor(Color.GREEN)
            .withFade(Color.BLUE)
            .with(FireworkEffect.Type.BALL)
            .withFlicker()
            .withTrail()
            .build();

        meta.addEffect(effect);
        meta.setPower(1);
        firework.setFireworkMeta(meta);
        firework.setSilent(true);
    }
}
