package me.nakilex.levelplugin.mob.listeners;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
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

    public MythicMobDeathListener(MobRewardsConfig mobRewardsConfig,
                                  LevelManager levelManager,
                                  EconomyManager economyManager) {
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

        // Debug: Confirm event is triggered

        if (mythicMob != null) {
            // Strip any color codes from the Mythic Mob type
            String rawMobType = mythicMob.getMobType();
            String mobType = rawMobType.replaceAll("§.", "");

            // Debug: Show raw vs. stripped

            // Check if this mob is in mob_rewards.yml
            if (mobRewardsConfig.getConfig().contains("mobs." + mobType)) {
                // 1) Give XP
                int exp = mobRewardsConfig.getConfig().getInt("mobs." + mobType + ".exp", 0);
                levelManager.addXP(player, exp);

                // 2) Give Coins
                String coinRange = mobRewardsConfig.getConfig().getString("mobs." + mobType + ".coins", "0-0");
                String[] coinsSplit = coinRange.split("-");
                int minCoins = Integer.parseInt(coinsSplit[0]);
                int maxCoins = Integer.parseInt(coinsSplit[1]);
                int coins = ThreadLocalRandom.current().nextInt(minCoins, maxCoins + 1);
                economyManager.addCoins(player, coins);

                // 3) Drop Custom Items (if any)
                dropCustomItems(player, mobType);

                // 4) Notify player
                player.sendMessage("You killed " + mobType + " and earned "
                    + exp + " XP and " + coins + " coins!");
            } else {
                // Debug: If it doesn't find that path
                System.out.println("[DEBUG] mob_rewards.yml has no path for: mobs." + mobType);
            }
        } else {
            // Debug: If the dead entity isn't recognized as a MythicMob
            System.out.println("[DEBUG] The killed entity is not a MythicMob instance.");
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
            System.out.println("[DEBUG] Path not found for: " + path);
            return;
        }

        // Retrieve list of drop entries (maps)
        List<Map<?, ?>> itemList = mobRewardsConfig.getConfig().getMapList(path);

        // Debug #2: Print the raw list we got from config
        System.out.println("[DEBUG] itemList for '" + mobType + "' => " + itemList);
        if (itemList == null || itemList.isEmpty()) {
            System.out.println("[DEBUG] itemList is null/empty. No drops to process.");
            return;
        }

        // Loop over each item entry in the list
        for (Map<?, ?> entry : itemList) {
            // Debug #3: Show each entry from config
            System.out.println("[DEBUG] Processing drop entry: " + entry);

            if (!entry.containsKey("itemid")) {
                System.out.println("[DEBUG] 'itemid' key missing in entry: " + entry);
                continue;
            }
            int itemId = (int) entry.get("itemid");

            // drop_rate
            double dropRate = entry.containsKey("drop_rate")
                ? (double) entry.get("drop_rate") : 100.0;
            double roll = ThreadLocalRandom.current().nextDouble() * 100.0;
            System.out.println("[DEBUG] dropRate=" + dropRate + ", roll=" + roll);
            if (roll > dropRate) {
                // Skip if random roll doesn't meet drop_rate
                System.out.println("[DEBUG] Skipping drop since roll (" + roll + ") > dropRate (" + dropRate + ")");
                continue;
            }

            // quantity
            String qtyRange = entry.containsKey("quantity")
                ? (String) entry.get("quantity") : "1-1";
            System.out.println("[DEBUG] quantity range=" + qtyRange);

            String[] rangeSplit = qtyRange.split("-");
            int minQty = Integer.parseInt(rangeSplit[0]);
            int maxQty = Integer.parseInt(rangeSplit[1]);
            int quantity = ThreadLocalRandom.current().nextInt(minQty, maxQty + 1);
            System.out.println("[DEBUG] Chosen quantity=" + quantity + " for itemId=" + itemId);

            // Fetch the base template from ItemManager
            CustomItem template = ItemManager.getInstance().getTemplateById(itemId);
            if (template == null) {
                player.sendMessage("§c[Warning] No CustomItem found with ID: " + itemId);
                System.out.println("[DEBUG] Template not found in ItemManager for itemId=" + itemId);
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
                    template.getHp(),
                    template.getDef(),
                    template.getStr(),
                    template.getAgi(),
                    template.getIntel(),
                    template.getDex()
                );

                // Add to ItemManager so it's recognized/tracked
                ItemManager.getInstance().addInstance(newInstance);

                // Convert CustomItem → ItemStack
                ItemStack dropStack = ItemUtil.createItemStackFromCustomItem(newInstance, 1, player);

                System.out.println("[DEBUG] Dropping item: " + newInstance.getBaseName()
                    + " with UUID=" + newInstance.getUuid());

                // Actually drop the item in the world
                player.getWorld().dropItemNaturally(player.getLocation(), dropStack);
            }
        }
    }
}
