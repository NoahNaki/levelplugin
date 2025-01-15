package me.nakilex.levelplugin.mob.listeners;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.concurrent.ThreadLocalRandom;

public class MythicMobDeathListener implements Listener {

    private final BukkitAPIHelper mythicHelper;
    private final MobRewardsConfig mobRewardsConfig;
    private final LevelManager levelManager;
    private final EconomyManager economyManager;

    public MythicMobDeathListener(MobRewardsConfig mobRewardsConfig, LevelManager levelManager, EconomyManager economyManager) {
        this.mythicHelper = MythicBukkit.inst().getAPIHelper();
        this.mobRewardsConfig = mobRewardsConfig;
        this.levelManager = levelManager;
        this.economyManager = economyManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player)) return;

        Player player = event.getEntity().getKiller();
        ActiveMob mythicMob = mythicHelper.getMythicMobInstance(event.getEntity());

        if (mythicMob != null) {
            String mobType = mythicMob.getMobType();

            // Check if the mob is in the config
            if (mobRewardsConfig.getConfig().contains("mobs." + mobType)) {
                int exp = mobRewardsConfig.getConfig().getInt("mobs." + mobType + ".exp");
                String coinRange = mobRewardsConfig.getConfig().getString("mobs." + mobType + ".coins");

                // Parse coin range and calculate random reward
                String[] range = coinRange.split("-");
                int minCoins = Integer.parseInt(range[0]);
                int maxCoins = Integer.parseInt(range[1]);
                int coins = ThreadLocalRandom.current().nextInt(minCoins, maxCoins + 1);

                // Add experience and coins
                levelManager.addXP(player, exp);
                economyManager.addCoins(player, coins);

                // Notify the player
                player.sendMessage("You killed " + mobType + " and earned " + exp + " XP and " + coins + " coins!");
            }
        }
    }
}
