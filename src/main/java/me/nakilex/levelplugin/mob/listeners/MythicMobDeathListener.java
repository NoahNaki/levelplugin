package me.nakilex.levelplugin.mob.listeners;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class MythicMobDeathListener implements Listener {

    private final BukkitAPIHelper mythicHelper;
    private final MobRewardsConfig mobRewardsConfig;
    private final LevelManager levelManager;
    private final EconomyManager economyManager;
    private final LootChestManager lootChestManager;

    public MythicMobDeathListener(MobRewardsConfig mobRewardsConfig,
                                  LevelManager levelManager,
                                  EconomyManager economyManager,
                                  LootChestManager lootChestManager) {
        this.mythicHelper = MythicBukkit.inst().getAPIHelper();
        this.mobRewardsConfig = mobRewardsConfig;
        this.levelManager = levelManager;
        this.economyManager = economyManager;
        this.lootChestManager  = lootChestManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // 1) Only proceed if a player killed the entity
        if (!(event.getEntity().getKiller() instanceof Player)) return;
        Player player = event.getEntity().getKiller();

        // 2) Try to get the MythicMob instance
        ActiveMob mythicMob = mythicHelper.getMythicMobInstance(event.getEntity());
        if (mythicMob == null) return;

        // 3) Strip any color codes from the mob type
        String rawMobType = mythicMob.getMobType();
        String mobType = rawMobType.replaceAll("§.", "");

        // 4) Ensure we have rewards data for this mob
        if (!mobRewardsConfig.getConfig().contains("mobs." + mobType)) return;

        // 5) Award XP
        int exp = mobRewardsConfig.getConfig().getInt("mobs." + mobType + ".exp", 0);
        levelManager.addXP(player, exp);

        // 6) Award Coins
        String coinRange = mobRewardsConfig.getConfig().getString("mobs." + mobType + ".coins", "0-0");
        String[] coinsSplit = coinRange.split("-");
        int minCoins = Integer.parseInt(coinsSplit[0]);
        int maxCoins = Integer.parseInt(coinsSplit[1]);
        int coins = ThreadLocalRandom.current().nextInt(minCoins, maxCoins + 1);
        economyManager.addCoins(player, coins);

        // 7) Drop any manually configured custom items
        dropCustomItems(player, mobType);

        // 8) Drop tier-based loot if tier > 0
        int tier = mobRewardsConfig.getConfig().getInt("mobs." + mobType + ".tier", 0);
        if (tier > 0) {
            ItemStack loot = lootChestManager.getRandomLootForTier(tier);
            if (loot != null) {
                // Update the tooltip before dropping
                ItemUtil.updateCustomItemTooltip(loot, player);
                player.getWorld().dropItemNaturally(player.getLocation(), loot);
            }
        }

        // 9) (Optional) Notify player of what they received
        // player.sendMessage("You killed " + mobType +
        //     " and earned " + exp + " XP, " + coins + " coins" +
        //     (tier > 0 ? " plus tier-" + tier + " loot!" : "!"));
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
