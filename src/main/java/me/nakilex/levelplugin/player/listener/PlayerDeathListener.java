// File: src/me/nakilex/levelplugin/player/listener/PlayerDeathListener.java
package me.nakilex.levelplugin.player.listener;

import me.nakilex.levelplugin.items.data.CustomItem;
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

        // Delay by 1 tick so that the player has fully respawned before reducing durability and possibly stripping stats
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                // 1) Give Blindness for ~3 seconds (60 ticks)
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false));

                // 2) Grab the item in their main hand and attempt to fetch the CustomItem
                ItemStack inHand = player.getInventory().getItemInMainHand();
                if (inHand == null || inHand.getType().isAir()) {
                    return; // nothing to modify
                }

                CustomItem cItem = ItemManager.getInstance().getCustomItemFromItemStack(inHand);
                if (cItem == null) {
                    return; // not a tracked custom item
                }

                int itemId = cItem.getId();
                Set<Integer> equipped = statsMgr.getEquippedItems(puuid);

                // 3) Reduce durability by 10% of MAX (i.e. 10 points if MAX=100)
                int amountToReduce = cItem.getMaxDurability() / 10;
                cItem.reduceDurability(amountToReduce);

                // 4) If the weapon is now broken and still marked as equipped, strip its stats
                if (cItem.isBroken() && equipped.contains(itemId)) {
                    Bukkit.getLogger().info(
                        "[PlayerDeathListener] Stripping stats on death for broken weapon ID="
                            + itemId + " (player=" + player.getName() + ")"
                    );
                    new WeaponStatsListener().removeWeaponStats(player, cItem);
                    equipped.remove(itemId);
                    ItemManager.getInstance().unregisterHolder(itemId);

                    // Recalculate derived stats immediately
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

                // 5) Re‐build the ItemStack (this now reflects “broken” if durability hit 0)
                ItemStack updatedStack = ItemUtil.createItemStackFromCustomItem(cItem, 1, player);
                player.getInventory().setItemInMainHand(updatedStack);

                // 6) If it broke just now, notify the player
                if (cItem.isBroken()) {
                    player.sendMessage(
                        ChatColor.RED + "Your “" + ChatColor.BOLD + cItem.getBaseName() + ChatColor.RED
                            + "” has broken!"
                    );
                }
            }
        }.runTaskLater(plugin, 1L);
    }
}
