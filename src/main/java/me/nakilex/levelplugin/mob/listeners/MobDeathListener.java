package me.nakilex.levelplugin.mob.listeners;

import me.nakilex.levelplugin.mob.managers.MobManager;
import me.nakilex.levelplugin.mob.data.MobConfig;
import me.nakilex.levelplugin.mob.data.CustomMob;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

public class MobDeathListener implements Listener {

    private MobManager mobManager;
    private EconomyManager economy;
    private Random random = new Random();

    public MobDeathListener(MobManager mobManager, EconomyManager economy) {
        this.mobManager = mobManager;
        this.economy = economy;
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        // Log when any mob dies
        String mobName = event.getEntity().getName();
        String killerName = (event.getEntity().getKiller() != null) ? event.getEntity().getKiller().getName() : "Unknown";
        System.out.println("EntityDeathEvent triggered for: Mob - " + mobName + ", Killed by: " + killerName);

        if (!(event.getEntity().getKiller() instanceof Player)) return;
        Player killer = event.getEntity().getKiller();

        // Check PDC for custom mob ID
        PersistentDataContainer pdc = event.getEntity().getPersistentDataContainer();
        if (!pdc.has(CustomMob.MOB_ID_KEY, PersistentDataType.STRING)) {
            System.out.println("Entity is not a custom mob.");
            return; // not a custom mob
        }
        String mobId = pdc.get(CustomMob.MOB_ID_KEY, PersistentDataType.STRING);
        MobConfig cfg = mobManager.getMobConfig(mobId);
        if (cfg == null) {
            System.out.println("No configuration found for mob ID: " + mobId);
            return;
        }

        event.getDrops().clear(); // clear vanilla drops

        for (MobConfig.LootEntry loot : cfg.getLootTable()) {
            int chance = random.nextInt(100) + 1;
            if (chance <= loot.getDropRate()) {
                if (loot.getItem().equalsIgnoreCase("COINS")) {
                    int coins = loot.getDropMin();
                    if (loot.getDropMax() > loot.getDropMin()) {
                        coins = loot.getDropMin() + random.nextInt((loot.getDropMax() - loot.getDropMin()) + 1);
                    }
                    // add to killer's balance
                    economy.addCoins(killer, coins);
                    killer.sendMessage(ChatColor.GREEN + "You received " + coins + " coins!");
                    System.out.println("Player " + killer.getName() + " received " + coins + " coins from mob " + mobName);
                } else {
                    // normal item drop
                    Material mat = Material.matchMaterial(loot.getItem());
                    if (mat != null) {
                        ItemStack stack = new ItemStack(mat, 1);
                        event.getDrops().add(stack);
                        System.out.println("Player " + killer.getName() + " received item " + loot.getItem() + " from mob " + mobName);
                    } else {
                        killer.sendMessage(ChatColor.YELLOW + "Mob dropped unknown item: " + loot.getItem());
                        System.out.println("Unknown item dropped: " + loot.getItem());
                    }
                }
            }
        }
    }
}
