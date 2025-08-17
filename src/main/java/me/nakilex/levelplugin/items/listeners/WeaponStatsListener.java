// File: src/me/nakilex/levelplugin/items/listeners/WeaponStatsListener.java
package me.nakilex.levelplugin.items.listeners;

import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.events.WeaponEquipEvent;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;

public class WeaponStatsListener implements Listener {

    private final StatsManager statsManager = StatsManager.getInstance();
    private final ItemManager itemManager = ItemManager.getInstance();

    @EventHandler
    public void onWeaponEquip(WeaponEquipEvent event) {
        // 0) Ignore canceled events or if neither old nor new is a weapon
        if (event.isCancelled()) return;
        if (WeaponType.matchType(event.getOldWeapon()) == null
            && WeaponType.matchType(event.getNewWeapon()) == null) {
            return;
        }

        Player player = event.getPlayer();
        UUID puuid = player.getUniqueId();
        StatsManager stats = StatsManager.getInstance();
        Set<Integer> equipped = stats.getEquippedItems(puuid);

        //
        // 1) REMOVE OLD WEAPON’S STATS (only if it’s still in “equipped” AND wasn’t broken)
        //
        ItemStack oldWeap = event.getOldWeapon();
        if (oldWeap != null && !oldWeap.getType().isAir()) {
            CustomItem inst = itemManager.getCustomItemFromItemStack(oldWeap);
            if (inst != null && equipped.contains(inst.getId())) {
                boolean wasBroken = inst.isBroken();
                StatsManager.PlayerStats ps = statsManager.getPlayerStats(puuid);

                Bukkit.getLogger().info(
                    "[WeaponStats] Removing old weapon (ID=" + inst.getId() +
                        ", broken=" + wasBroken + ") for player " + player.getName()
                );
                logPlayerStats("Before removal", puuid, ps);

                if (!wasBroken) {
                    // Subtract its stats now
                    removeWeaponStats(player, inst, oldWeap);
                    // Remove from the “equipped” set
                    equipped.remove(inst.getId());
                    // Unregister from holderMap so reduceDurability() can’t find it later
                    itemManager.unregisterHolder(inst.getId());
                    Bukkit.getLogger().info(
                        "[WeaponStats] Removed stats for weapon ID=" + inst.getId()
                    );
                } else {
                    Bukkit.getLogger().info(
                        "[WeaponStats] Skipped removal because weapon was already broken."
                    );
                }

                logPlayerStats("After removal", puuid, ps);
            }
        }

        //
        // 2) ADD NEW WEAPON’S STATS (only if not already in “equipped,” not broken, and meets reqs)
        //
        ItemStack newWeap = event.getNewWeapon();
        if (newWeap != null && !newWeap.getType().isAir()) {
            // Skip applying stats if the item being held is actually armor.
            // Armor bonuses should only apply when equipped via ArmorListener,
            // not just by holding the piece in hand.
            if (me.nakilex.levelplugin.items.data.ArmorType.matchType(newWeap) != null) {
                Bukkit.getLogger().info("[WeaponStats] Skipping addition because new item is armor.");
            } else {
                CustomItem inst = itemManager.getCustomItemFromItemStack(newWeap);
                if (inst != null && !equipped.contains(inst.getId())) {
                    boolean isBroken   = inst.isBroken();
                    int playerLevel    = LevelManager.getInstance().getLevel(player);
                    int requiredLevel  = inst.getLevelRequirement();

                    String clsReqRaw = inst.getClassRequirement();
                    me.nakilex.levelplugin.player.classes.data.PlayerClass reqClass = null;
                    try {
                        if (clsReqRaw != null && !clsReqRaw.isBlank()) {
                            reqClass = me.nakilex.levelplugin.player.classes.data.PlayerClass.valueOf(clsReqRaw.toUpperCase());
                        }
                    } catch (IllegalArgumentException ignored) {}

                    StatsManager.PlayerStats ps = statsManager.getPlayerStats(puuid);
                    me.nakilex.levelplugin.player.classes.data.PlayerClass playerClass = ps.playerClass;

                Bukkit.getLogger().info(
                    "[WeaponStats] Attempting to add new weapon (ID=" + inst.getId() +
                        ", broken=" + isBroken + ") for player " + player.getName()
                );
                logPlayerStats("Before addition", puuid, ps);

                if (isBroken) {
                    Bukkit.getLogger().info("[WeaponStats] Skipped addition because weapon is broken.");
                    player.sendMessage(ChatColor.YELLOW
                        + inst.getBaseName()
                        + " is broken and grants no bonuses."
                    );
                }
                else if (!me.nakilex.levelplugin.player.classes.data.ClassUtil.meetsRequirement(playerClass, reqClass)) {
                    Bukkit.getLogger().info(
                        "[WeaponStats] Skipped addition: player class "
                            + playerClass + " does not meet required " + reqClass
                    );
                    player.sendMessage(ChatColor.RED
                        + "You are not the right class to gain stats from "
                        + inst.getBaseName() + "."
                    );
                }
                else if (playerLevel < requiredLevel) {
                    Bukkit.getLogger().info(
                        "[WeaponStats] Skipped addition: player level "
                            + playerLevel + " < required level " + requiredLevel + "."
                    );
                    player.sendMessage(ChatColor.RED
                        + "You can hold "
                        + inst.getBaseName()
                        + " but lack the level to gain its stats."
                    );
                }
                else {
                    // Add stats now
                    addWeaponStats(player, inst, newWeap);
                    // Put it into the “equipped” set
                    equipped.add(inst.getId());
                    // Register in holderMap so CustomItem.reduceDurability can find it
                    itemManager.registerHolder(inst.getId(), puuid);
                    Bukkit.getLogger().info(
                        "[WeaponStats] Added stats for weapon ID=" + inst.getId()
                    );
                }

                    logPlayerStats("After addition", puuid, ps);
                }
            }
        }

        //
        // 3) Always recalc derived stats so changes are immediate
        //
        stats.recalcDerivedStats(player);
    }


    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID puuid = player.getUniqueId();
        Set<Integer> equipped = statsManager.getEquippedItems(puuid);

        Bukkit.getLogger().info("[WeaponStats] onPlayerRespawn fired for player " + player.getName());
        Bukkit.getLogger().info("[WeaponStats] Equipped IDs before respawn‐cleanup: " + equipped);

        // 1) Check the CustomItem in main hand (if any)
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null || inHand.getType().isAir()) {
            Bukkit.getLogger().info("[WeaponStats] onPlayerRespawn: main hand empty or air.");
            return;
        }

        CustomItem inst = itemManager.getCustomItemFromItemStack(inHand);
        if (inst == null) {
            Bukkit.getLogger().info("[WeaponStats] onPlayerRespawn: not a CustomItem.");
            return;
        }

        int id = inst.getId();
        boolean broken = inst.isBroken();

        Bukkit.getLogger().info("[WeaponStats] onPlayerRespawn: found ID=" + id + ", isBroken=" + broken);

        // 2) If the item is broken, check if its stats are still applied
        if (broken) {
            StatsManager.PlayerStats ps = statsManager.getPlayerStats(puuid);

            int vitVal = inst.getHp() + inst.getDef();
            int strVal = inst.getStr();
            int agiVal = inst.getAgi();
            int intVal = inst.getIntel();
            int dexVal = inst.getDex();

            boolean hasVitBonus = ps.bonusVitality     >= vitVal;
            boolean hasStrBonus = ps.bonusStrength     >= strVal;
            boolean hasAgiBonus = ps.bonusAgility      >= agiVal;
            boolean hasIntBonus = ps.bonusIntelligence >= intVal;
            boolean hasDexBonus = ps.bonusDexterity    >= dexVal;

            // If the player still has at least all of the item's bonuses, subtract them once.
            if (hasVitBonus && hasStrBonus && hasAgiBonus && hasIntBonus && hasDexBonus) {
                Bukkit.getLogger().info(
                    "[WeaponStats] onPlayerRespawn: Removing stats for broken weapon ID=" + id
                        + " (player=" + player.getName() + ")"
                );
                removeWeaponStats(player, inst, inHand);

                // Also remove from equipped set (if present) and unregister holder
                if (equipped.contains(id)) {
                    equipped.remove(id);
                    itemManager.unregisterHolder(id);
                }

                // Log “after” and recalc
                logPlayerStats("After respawn removal", puuid, ps);
                statsManager.recalcDerivedStats(player);
            } else {
                Bukkit.getLogger().info(
                    "[WeaponStats] onPlayerRespawn: Broken weapon ID=" + id
                        + " but stats not present (cannot remove). ps="
                        + "{vit=" + ps.bonusVitality
                        + ", vit="   + ps.bonusVitality
                        + ", str="   + ps.bonusStrength
                        + ", agi="   + ps.bonusAgility
                        + ", intel="+ ps.bonusIntelligence
                        + ", dex="   + ps.bonusDexterity + "}"
                );
            }
        } else {
            Bukkit.getLogger().info(
                "[WeaponStats] onPlayerRespawn: ID=" + id
                    + " is NOT broken → no action."
            );
        }
    }




    public void addWeaponStats(Player player, CustomItem customItem, ItemStack stack) {
        StatsManager.PlayerStats ps = statsManager.getPlayerStats(player.getUniqueId());
        ps.bonusVitality     += customItem.getHp() + customItem.getDef();
        ps.bonusStrength     += customItem.getStr();
        ps.bonusAgility      += customItem.getAgi();
        ps.bonusIntelligence += customItem.getIntel();
        ps.bonusDexterity    += customItem.getDex();
        ps.bonusWill         += customItem.getWil();
    }

    public void removeWeaponStats(Player player, CustomItem customItem, ItemStack stack) {
        Bukkit.getLogger().info("[WeaponStats] removeWeaponStats() called for player="
            + player.getName() + ", itemID=" + customItem.getId());

        StatsManager.PlayerStats ps = statsManager.getPlayerStats(player.getUniqueId());
        ps.bonusVitality     = Math.max(0, ps.bonusVitality - (customItem.getHp() + customItem.getDef()));
        ps.bonusStrength     = Math.max(0, ps.bonusStrength     - customItem.getStr());
        ps.bonusAgility      = Math.max(0, ps.bonusAgility      - customItem.getAgi());
        ps.bonusIntelligence = Math.max(0, ps.bonusIntelligence - customItem.getIntel());
        ps.bonusDexterity    = Math.max(0, ps.bonusDexterity    - customItem.getDex());
        ps.bonusWill         = Math.max(0, ps.bonusWill         - customItem.getWil());

        // IMMEDIATELY recalc all derived stats (this will:
        //  • recompute maxHealth → setMaxHealth(...)
        //  • recompute maxMana  → (and clamp currentMana if needed)
        StatsManager.getInstance().recalcDerivedStats(player);

        Bukkit.getLogger().info("[WeaponStats] After removeWeaponStats, stats => "
            + "bonusVitality="     + ps.bonusVitality
            + ", bonusVitality="   + ps.bonusVitality
            + ", bonusStrength="   + ps.bonusStrength
            + ", bonusAgility="    + ps.bonusAgility
            + ", bonusIntelligence="+ ps.bonusIntelligence
            + ", bonusDexterity="  + ps.bonusDexterity
        );
    }

    /**
     * Logs every bonus‐stat field on a player's PlayerStats.
     */
    private void logPlayerStats(String prefix, UUID puuid, StatsManager.PlayerStats ps) {
        Bukkit.getLogger().info(
            "[WeaponStats] "
                + prefix
                + " stats for player UUID="
                + puuid
                + " => bonusVitality="
                + ps.bonusVitality
                + ", bonusDefence="
                + ps.bonusVitality
                + ", bonusStrength="
                + ps.bonusStrength
                + ", bonusAgility="
                + ps.bonusAgility
                + ", bonusIntelligence="
                + ps.bonusIntelligence
                + ", bonusDexterity="
                + ps.bonusDexterity
        );
    }
}
