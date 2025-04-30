package me.nakilex.levelplugin.mob.listeners;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class MythicMobDeathListener implements Listener {

    private final BukkitAPIHelper mythicHelper;
    private final MobRewardsConfig mobRewardsConfig;
    private final LevelManager levelManager;
    private final EconomyManager economyManager;
    private final LootChestManager lootChestManager;

    // ← New field: track which players damaged each mob
    private final Map<UUID, Set<Player>> damageTracker = new ConcurrentHashMap<>();

    public MythicMobDeathListener(MobRewardsConfig mobRewardsConfig,
                                  LevelManager levelManager,
                                  EconomyManager economyManager,
                                  LootChestManager lootChestManager) {
        this.mythicHelper      = MythicBukkit.inst().getAPIHelper();
        this.mobRewardsConfig  = mobRewardsConfig;
        this.levelManager      = levelManager;
        this.economyManager    = economyManager;
        this.lootChestManager  = lootChestManager;
    }

    // ← New method: record every player who hits a mob
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // only care about living‐entity mobs
        if (!(event.getEntity() instanceof LivingEntity)) return;

        Player hitter = null;
        // 1) direct melee
        if (event.getDamager() instanceof Player) {
            hitter = (Player) event.getDamager();
        }
        // 2) projectile hit
        else if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                hitter = (Player) proj.getShooter();
            }
        }

        if (hitter == null) return;

        // record participation
        LivingEntity mob = (LivingEntity) event.getEntity();
        damageTracker
            .computeIfAbsent(mob.getUniqueId(), uuid -> ConcurrentHashMap.newKeySet())
            .add(hitter);
    }

    // ← Modified death handler: reward each participant client-side
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        ActiveMob mythicMob = mythicHelper.getMythicMobInstance(event.getEntity());
        if (mythicMob == null) return;

        String mobType = mythicMob.getMobType().replaceAll("§.", "");
        if (!mobRewardsConfig.getConfig().contains("mobs." + mobType)) return;

        // Pull config once
        ConfigurationSection node = mobRewardsConfig
            .getConfig()
            .getConfigurationSection("mobs." + mobType);
        int exp        = node.getInt("exp", 0);
        String coinsSpec = node.getString("coins", "0-0");
        int tier       = node.getInt("tier", 0);

        String[] sp     = coinsSpec.split("-");
        int minCoins    = Integer.parseInt(sp[0]);
        int maxCoins    = Integer.parseInt(sp[1]);

        // Who participated?
        Set<Player> participants = damageTracker
            .getOrDefault(event.getEntity().getUniqueId(), Collections.emptySet());
        damageTracker.remove(event.getEntity().getUniqueId());

        for (Player player : participants) {
            // 1) XP
            levelManager.addXP(player, exp);

            // 2) Coins
            int coins = ThreadLocalRandom.current().nextInt(minCoins, maxCoins + 1);
            economyManager.addCoins(player, coins);

            // 3) Manual custom-item drops
            dropCustomItems(player, mobType);

            // 4) Tier-based loot if tier > 0
            if (tier > 0) {
                ItemStack loot = lootChestManager.getRandomLootForTier(tier);
                if (loot != null) {
                    // update tooltip for this player
                    ItemUtil.updateCustomItemTooltip(loot, player);
                    // give directly to inventory, fallback to drop if full
                    Map<Integer, ItemStack> leftovers = player.getInventory().addItem(loot);
                    leftovers.values().forEach(i ->
                        player.getWorld().dropItemNaturally(player.getLocation(), i)
                    );
                }
            }

            // 5) Optional feedback
            player.sendMessage(
                "§aYou earned " + exp + " XP and " + coins +
                    " coins" + (tier > 0 ? " plus tier-" + tier + " loot!" : "!"));
        }
    }



    /**
     * Reads `mobs.<mobType>.items` from mob_rewards.yml,
     * and drops items based on drop_rate and quantity ranges.
     * This version includes DEBUG logs to help troubleshoot issues.
     */
    private void dropCustomItems(Player player, String mobType) {
        String path = "mobs." + mobType + ".items";

        // Debug #1: Check if the path is in config
        System.out.println("[DEBUG] Checking config path: " + path
            + " => " + mobRewardsConfig.getConfig().contains(path));
        if (!mobRewardsConfig.getConfig().contains(path)) {
            //System.out.println("[DEBUG] Path not found for: " + path);
            return;
        }

        // Retrieve list of drop entries (maps)
        List<Map<?, ?>> itemList = mobRewardsConfig.getConfig().getMapList(path);

        // Debug #2: Print the raw list we got from config
        System.out.println("[DEBUG] itemList for '" + mobType + "' => " + itemList);
        if (itemList == null || itemList.isEmpty()) {
            //System.out.println("[DEBUG] itemList is null/empty. No drops to process.");
            return;
        }

        // Loop over each item entry in the list
        for (Map<?, ?> entry : itemList) {
            // Debug #3: Show each entry from config
            System.out.println("[DEBUG] Processing drop entry: " + entry);

            if (!entry.containsKey("itemid")) {
                //System.out.println("[DEBUG] 'itemid' key missing in entry: " + entry);
                continue;
            }
            int itemId = (int) entry.get("itemid");

            // drop_rate
            double dropRate = entry.containsKey("drop_rate")
                ? (double) entry.get("drop_rate") : 100.0;
            double roll = ThreadLocalRandom.current().nextDouble() * 100.0;
            //System.out.println("[DEBUG] dropRate=" + dropRate + ", roll=" + roll);
            if (roll > dropRate) {
                // Skip if random roll doesn't meet drop_rate
                //System.out.println("[DEBUG] Skipping drop since roll (" + roll + ") > dropRate (" + dropRate + ")");
                continue;
            }

            // quantity
            String qtyRange = entry.containsKey("quantity")
                ? (String) entry.get("quantity") : "1-1";
            //System.out.println("[DEBUG] quantity range=" + qtyRange);

            String[] rangeSplit = qtyRange.split("-");
            int minQty = Integer.parseInt(rangeSplit[0]);
            int maxQty = Integer.parseInt(rangeSplit[1]);
            int quantity = ThreadLocalRandom.current().nextInt(minQty, maxQty + 1);
            //System.out.println("[DEBUG] Chosen quantity=" + quantity + " for itemId=" + itemId);

            // Fetch the base template from ItemManager
            CustomItem template = ItemManager.getInstance().getTemplateById(itemId);
            if (template == null) {
                player.sendMessage("§c[Warning] No CustomItem found with ID: " + itemId);
                //System.out.println("[DEBUG] Template not found in ItemManager for itemId=" + itemId);
                continue;
            }

            // For each item in the chosen quantity, create and drop a new instance
            for (int i = 0; i < quantity; i++) {
                CustomItem newInstance = new CustomItem(
                    template.getId(),
                    template.getBaseName(),
                    template.getRarity(),
                    template.getLevelRequirement(),
                    template.getClassRequirement(),
                    template.getMaterial(),
                    template.getHpRange(),
                    template.getDefRange(),
                    template.getStrRange(),
                    template.getAgiRange(),
                    template.getIntelRange(),
                    template.getDexRange()
                );

                // Add to ItemManager so it's recognized/tracked
                ItemManager.getInstance().addInstance(newInstance);

                // Convert CustomItem → ItemStack
                ItemStack dropStack = ItemUtil.createItemStackFromCustomItem(newInstance, 1, player);
                ItemUtil.updateCustomItemTooltip(dropStack, player);

                player.getWorld().dropItemNaturally(player.getLocation(), dropStack);
            }
        }
    }
}
