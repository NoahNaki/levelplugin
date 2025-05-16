package me.nakilex.levelplugin.mob.listeners;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.ChatColor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class MythicMobDeathListener implements Listener {

    private final BukkitAPIHelper mythicHelper;
    private final MobRewardsConfig mobRewardsConfig;
    private final LevelManager levelManager;
    private final EconomyManager economyManager;
    private final LootChestManager lootChestManager;
    private final Main plugin = Main.getInstance();
    private static final Set<UUID> dropDetailsDisabled = new HashSet<>();


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
        // Get the MythicMob wrapper
        ActiveMob mythicMob = mythicHelper.getMythicMobInstance(event.getEntity());
        if (mythicMob == null) return;

        // Normalize the mob type and ensure it’s in our config
        String mobType = mythicMob.getMobType().replaceAll("§.", "");
        if (!mobRewardsConfig.getConfig().contains("mobs." + mobType)) return;

        // Load common reward values
        ConfigurationSection node     = mobRewardsConfig
            .getConfig()
            .getConfigurationSection("mobs." + mobType);
        int exp                      = node.getInt("exp", 0);
        String coinsSpec             = node.getString("coins", "0-0");
        int tier                     = node.getInt("tier", 0);
        double tierChance            = node.getDouble("tier_chance", 100.0);

        // Parse coin range
        String[] sp      = coinsSpec.split("-");
        int minCoins     = Integer.parseInt(sp[0]);
        int maxCoins     = Integer.parseInt(sp[1]);

        // Determine who hit this mob
        Set<Player> participants = damageTracker
            .getOrDefault(event.getEntity().getUniqueId(), Collections.emptySet());
        damageTracker.remove(event.getEntity().getUniqueId());

        // Reward each participant
        for (Player player : participants) {
            // 1) XP
            levelManager.addXP(player, exp);

            // 2) Coins
            int coins = ThreadLocalRandom.current().nextInt(minCoins, maxCoins + 1);
            economyManager.addCoins(player, coins);

            // 3) Custom‐item drops
            dropCustomItems(player, mobType);

            // 4) Tier‐loot, but only if the roll ≤ tierChance
            if (tier > 0) {
                double roll = ThreadLocalRandom.current().nextDouble() * 100.0;
                if (roll <= tierChance) {
                    ItemStack loot = lootChestManager.getRandomLootForTier(tier);
                    if (loot != null) {
                        ItemUtil.updateCustomItemTooltip(loot, player);
                        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(loot);
                        leftovers.values().forEach(i ->
                            player.getWorld().dropItemNaturally(player.getLocation(), i)
                        );
                    }
                }
            }

            if (MythicMobDeathListener.isDropDetailsEnabled(player)) {
                Location deathLoc = event.getEntity().getLocation();
                showRewardHologram(deathLoc, exp, coins);
            }
            player.sendMessage(
                "§aYou earned " + exp + " XP and " + coins + " coins");
        }
    }


    // New signature: pass in the world location where you want the hologram
    private void showRewardHologram(Location loc, int xp, int coins) {
        // Base location just above the ground
        loc = loc.clone().add(0, 1.2, 0);

        // 1) XP line
        String xpLine = ChatColor.GRAY + "["
            + ChatColor.WHITE + "+" + xp + " "
            + ChatColor.GREEN  + "XP"
            + ChatColor.GRAY + "]";

        ArmorStand xpStand = loc.getWorld().spawn(loc, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setMarker(true);
            as.setCustomNameVisible(true);
            as.setCustomName(xpLine);
        });

        // 2) Coins line, half a block below
        String coinLine = ChatColor.GRAY + "["
            + ChatColor.WHITE + "+" + coins + " "
            + ChatColor.GOLD   + "⛃"
            + ChatColor.GRAY + "]";

        Location coinLoc = loc.clone().add(0, -0.3, 0);
        ArmorStand coinStand = coinLoc.getWorld().spawn(coinLoc, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setMarker(true);
            as.setCustomNameVisible(true);
            as.setCustomName(coinLine);
        });

        // remove both after 2 seconds (40 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!xpStand.isDead())  xpStand.remove();
                if (!coinStand.isDead()) coinStand.remove();
            }
        }.runTaskLater(plugin, 40L);
    }

    public static boolean isDropDetailsEnabled(Player p) {
        return !dropDetailsDisabled.contains(p.getUniqueId());
    }
    public static void toggleDropDetails(Player p) {
        UUID u = p.getUniqueId();
        if (dropDetailsDisabled.contains(u)) dropDetailsDisabled.remove(u);
        else dropDetailsDisabled.add(u);
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
