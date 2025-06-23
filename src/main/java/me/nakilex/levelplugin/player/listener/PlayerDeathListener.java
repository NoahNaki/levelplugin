// File: src/me/nakilex/levelplugin/player/listener/PlayerDeathListener.java
package me.nakilex.levelplugin.player.listener;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.listeners.ArmorStatsListener;
import me.nakilex.levelplugin.items.listeners.WeaponStatsListener;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.UUID;

public class PlayerDeathListener implements Listener {

    private final JavaPlugin plugin;

    public PlayerDeathListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID puuid = player.getUniqueId();
        StatsManager statsMgr = StatsManager.getInstance();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                // 1) Give Blindness for ~3 seconds (60 ticks)
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false));

                // ───────────────────────────────────────────────────────────────────────────────
                // 2) Reduce durability of each equipped armor piece (helmet, chestplate, leggings, boots)
                //    If any break, strip their stats and recalc immediately so HP/mana update.
                // ───────────────────────────────────────────────────────────────────────────────

                // (a) HELMET
                ItemStack helmet = player.getInventory().getHelmet();
                if (helmet != null && !helmet.getType().isAir()) {
                    CustomItem armorItem = ItemManager.getInstance().getCustomItemFromItemStack(helmet);
                    if (armorItem != null) {
                        int armorId = armorItem.getId();
                        int reduceAmtArmor = armorItem.getMaxDurability() / 10;
                        armorItem.reduceDurability(reduceAmtArmor, player, helmet);

                        // Re‐build the helmet ItemStack to reflect new durability
                        ItemStack updatedHelmet = ItemUtil.createItemStackFromCustomItem(armorItem, 1, player);
                        ItemUtil.copyEgoData(helmet, updatedHelmet, armorItem, player);
                        player.getInventory().setHelmet(updatedHelmet);

                        if (armorItem.isBroken()) {
                            new ArmorStatsListener().removeItemStats(player, armorItem);
                            ItemManager.getInstance().unregisterHolder(armorId);
                            statsMgr.recalcDerivedStats(player);

                            player.sendMessage(
                                ChatColor.RED + "Your “" + ChatColor.BOLD
                                    + armorItem.getBaseName() + ChatColor.RED + "” has broken!"
                            );
                        }
                    }
                }

                // (b) CHESTPLATE
                ItemStack chest = player.getInventory().getChestplate();
                if (chest != null && !chest.getType().isAir()) {
                    CustomItem armorItem = ItemManager.getInstance().getCustomItemFromItemStack(chest);
                    if (armorItem != null) {
                        int armorId = armorItem.getId();
                        int reduceAmtArmor = armorItem.getMaxDurability() / 10;
                        armorItem.reduceDurability(reduceAmtArmor, player, chest);

                        ItemStack updatedChest = ItemUtil.createItemStackFromCustomItem(armorItem, 1, player);
                        ItemUtil.copyEgoData(chest, updatedChest, armorItem, player);
                        player.getInventory().setChestplate(updatedChest);

                        if (armorItem.isBroken()) {
                            new ArmorStatsListener().removeItemStats(player, armorItem);
                            ItemManager.getInstance().unregisterHolder(armorId);
                            statsMgr.recalcDerivedStats(player);

                            player.sendMessage(
                                ChatColor.RED + "Your “" + ChatColor.BOLD
                                    + armorItem.getBaseName() + ChatColor.RED + "” has broken!"
                            );
                        }
                    }
                }

                // (c) LEGGINGS
                ItemStack legs = player.getInventory().getLeggings();
                if (legs != null && !legs.getType().isAir()) {
                    CustomItem armorItem = ItemManager.getInstance().getCustomItemFromItemStack(legs);
                    if (armorItem != null) {
                        int armorId = armorItem.getId();
                        int reduceAmtArmor = armorItem.getMaxDurability() / 10;
                        armorItem.reduceDurability(reduceAmtArmor, player, legs);

                        ItemStack updatedLegs = ItemUtil.createItemStackFromCustomItem(armorItem, 1, player);
                        ItemUtil.copyEgoData(legs, updatedLegs, armorItem, player);
                        player.getInventory().setLeggings(updatedLegs);

                        if (armorItem.isBroken()) {
                            new ArmorStatsListener().removeItemStats(player, armorItem);
                            ItemManager.getInstance().unregisterHolder(armorId);
                            statsMgr.recalcDerivedStats(player);

                            player.sendMessage(
                                ChatColor.RED + "Your “" + ChatColor.BOLD
                                    + armorItem.getBaseName() + ChatColor.RED + "” has broken!"
                            );
                        }
                    }
                }

                // (d) BOOTS
                ItemStack boots = player.getInventory().getBoots();
                if (boots != null && !boots.getType().isAir()) {
                    CustomItem armorItem = ItemManager.getInstance().getCustomItemFromItemStack(boots);
                    if (armorItem != null) {
                        int armorId = armorItem.getId();
                        int reduceAmtArmor = armorItem.getMaxDurability() / 10;
                        armorItem.reduceDurability(reduceAmtArmor, player, boots);

                        ItemStack updatedBoots = ItemUtil.createItemStackFromCustomItem(armorItem, 1, player);
                        ItemUtil.copyEgoData(boots, updatedBoots, armorItem, player);
                        player.getInventory().setBoots(updatedBoots);

                        if (armorItem.isBroken()) {
                            new ArmorStatsListener().removeItemStats(player, armorItem);
                            ItemManager.getInstance().unregisterHolder(armorId);
                            statsMgr.recalcDerivedStats(player);

                            player.sendMessage(
                                ChatColor.RED + "Your “" + ChatColor.BOLD
                                    + armorItem.getBaseName() + ChatColor.RED + "” has broken!"
                            );
                        }
                    }
                }

                // ───────────────────────────────────────────────────────────────────────────────
                // 3) Grab the item in their main hand and attempt to fetch the CustomItem
                //    Only if it’s a tracked CustomItem do we touch weapon durability and stats.
                // ───────────────────────────────────────────────────────────────────────────────

                ItemStack inHand = player.getInventory().getItemInMainHand();
                if (inHand != null && !inHand.getType().isAir()) {
                    CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(inHand);
                    if (cItem != null) {
                        int itemId = cItem.getId();

                        // 4) Reduce durability by 10% of MAX (i.e. 10 points if MAX=100)
                        int amountToReduce = cItem.getMaxDurability() / 10;
                        cItem.reduceDurability(amountToReduce, player, inHand);

                        // 5) If the weapon is now broken and still marked as equipped, strip its stats
                        Set<Integer> equipped = statsMgr.getEquippedItems(puuid);
                        if (cItem.isBroken() && equipped.contains(itemId)) {
                            Bukkit.getLogger().info(
                                "[PlayerDeathListener] Stripping stats on death for broken weapon ID="
                                    + itemId + " (player=" + player.getName() + ")"
                            );
                            new WeaponStatsListener().removeWeaponStats(player, cItem, inHand);
                            equipped.remove(itemId);
                            ItemManager.getInstance().unregisterHolder(itemId);

                            // Recalculate derived stats immediately (HP, mana will update)
                            statsMgr.recalcDerivedStats(player);

                            // (Optional) Log “after” stats for debugging
                            StatsManager.PlayerStats psAfter = statsMgr.getPlayerStats(puuid);
                            Bukkit.getLogger().info(
                                "[PlayerDeathListener] After stripping, stats => "
                                    + "bonusHealth="       + psAfter.bonusHealthStat
                                    + ", bonusDefence="    + psAfter.bonusDefenceStat
                                    + ", bonusStrength="   + psAfter.bonusStrength
                                    + ", bonusAgility="    + psAfter.bonusAgility
                                    + ", bonusIntelligence="+ psAfter.bonusIntelligence
                                    + ", bonusDexterity="  + psAfter.bonusDexterity
                            );
                        }

                        // 6) Re‐build the ItemStack (this now reflects “broken” if durability hit 0)
                        ItemStack updatedStack = ItemUtil.createItemStackFromCustomItem(cItem, 1, player);
                        ItemUtil.copyEgoData(inHand, updatedStack, cItem, player);
                        player.getInventory().setItemInMainHand(updatedStack);

                        // 7) If it broke just now, notify the player
                        if (cItem.isBroken()) {
                            player.sendMessage(
                                ChatColor.RED + "Your “" + ChatColor.BOLD + cItem.getBaseName() + ChatColor.RED
                                    + "” has broken!"
                            );
                        }
                    }
                }
            }
        }.runTaskLater(plugin, 1L);
    }
}
