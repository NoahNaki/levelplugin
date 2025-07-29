package me.nakilex.levelplugin.mob.listeners;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.mob.config.ModelSetManager;
import me.nakilex.levelplugin.mob.utils.DropDisplayToggles;
import me.nakilex.levelplugin.mob.utils.ItemDropper;
import me.nakilex.levelplugin.mob.utils.RewardHologramUtil;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.utils.ItemUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Awards XP, coins and loot to players that participated in killing MythicMobs.
 */
public class MythicMobRewardListener implements Listener {
    private final BukkitAPIHelper mythicHelper = MythicBukkit.inst().getAPIHelper();
    private final MobRewardsConfig mobRewardsConfig;
    private final LevelManager levelManager;
    private final EconomyManager economyManager;
    private final LootChestManager lootChestManager;
    private final ModelSetManager modelSetManager;
    private final MythicMobDamageTracker tracker;
    private final ItemDropper itemDropper;

    public MythicMobRewardListener(MythicMobDamageTracker tracker,
                                   MobRewardsConfig mobRewardsConfig,
                                   LevelManager levelManager,
                                   EconomyManager economyManager,
                                   LootChestManager lootChestManager,
                                   ModelSetManager modelSetManager) {
        this.tracker = tracker;
        this.mobRewardsConfig = mobRewardsConfig;
        this.levelManager = levelManager;
        this.economyManager = economyManager;
        this.lootChestManager = lootChestManager;
        this.modelSetManager = modelSetManager;
        this.itemDropper = new ItemDropper(levelManager, mobRewardsConfig, modelSetManager);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        ActiveMob mythicMob = mythicHelper.getMythicMobInstance(event.getEntity());
        if (mythicMob == null) return;
        String mobType = mythicMob.getMobType().replaceAll("§.", "");
        if (!mobRewardsConfig.getConfig().contains("mobs." + mobType)) return;
        ConfigurationSection node = mobRewardsConfig.getConfig().getConfigurationSection("mobs." + mobType);
        int exp = node.getInt("exp", 0);
        String coinsSpec = node.getString("coins", "0-0");
        int tier = node.getInt("tier", 0);
        double tierChance = node.getDouble("tier_chance", 100.0);
        String modelSet = node.getString("model_set", null);
        String[] sp = coinsSpec.split("-");
        int minCoins = Integer.parseInt(sp[0]);
        int maxCoins = Integer.parseInt(sp[1]);

        Set<Player> participants = tracker.getParticipantsAndClear(event.getEntity().getUniqueId());
        if (participants.isEmpty()) participants = Collections.emptySet();

        for (Player player : participants) {
            PartyManager pm = Main.getInstance().getPartyManager();
            Party party = pm.getParty(player.getUniqueId());
            int bonusPercent = 0;
            if (party != null) {
                int size = party.getSize();
                bonusPercent = Math.min(Math.max(size - 1, 0), 3) * 10;
            }
            int awardedExp = exp + (exp * bonusPercent) / 100;
            levelManager.addXP(player, awardedExp);
            int coins = ThreadLocalRandom.current().nextInt(minCoins, maxCoins + 1);
            economyManager.addCoins(player, coins);
            itemDropper.dropCustomItems(player, mobType, modelSet);
            if (tier > 0) {
                double roll = ThreadLocalRandom.current().nextDouble() * 100.0;
                if (roll <= tierChance) {
                    ItemStack loot = lootChestManager.getRandomLootForTier(tier, mobType, modelSet);
                    if (loot != null) {
                        ItemUtil.updateTooltip(loot, player);
                        player.getInventory().addItem(loot).values()
                                .forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
                    }
                }
            }
            itemDropper.maybeDropRerollScroll(player);
            if (DropDisplayToggles.isDropDetailsEnabled(player)) {
                Location deathLoc = event.getEntity().getLocation();
                RewardHologramUtil.showRewardHologram(deathLoc, awardedExp, coins);
            }
            if (DropDisplayToggles.isChatEnabled(player)) {
                String expLabel = net.md_5.bungee.api.ChatColor.of("#47b587") + "EXP";
                player.sendMessage("§7You earned §f+" + awardedExp + " <glyph:experience_orb_icon> " + expLabel
                        + " §7and §f+" + coins + " <glyph:coins_icon> §6coins");
            }
        }
    }
}
