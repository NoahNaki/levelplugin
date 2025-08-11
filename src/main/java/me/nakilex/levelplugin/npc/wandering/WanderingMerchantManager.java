package me.nakilex.levelplugin.npc.wandering;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.salvage.managers.SalvageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Spawns a wandering merchant NPC with a llama companion.
 * Generates a random shop inventory when spawned.
 */
public class WanderingMerchantManager {
    private final Main plugin;
    private WanderingTrader merchant;
    private TraderLlama llama1;
    private TraderLlama llama2;
    private WanderingMerchantGUI gui;
    private int shopGearScore;
    private long lastSpawn = 0L;
    private long lastDamage = 0L;
    private java.util.UUID lastAttacker;
    private org.bukkit.scheduler.BukkitTask fleeTask;
    private org.bukkit.scheduler.BukkitTask followTask;
    private TraderLlama ensureLlama() {
        if (llama1 != null && llama1.isValid()) {
            llama1.setGravity(true);
            llama1.setRemoveWhenFarAway(false);
            llama1.setAI(true);
            return llama1;
        }
        // attempt to reuse second llama if alive
        if (llama2 != null && llama2.isValid()) {
            llama2.setGravity(true);
            llama2.setRemoveWhenFarAway(false);
            llama2.setAI(true);
            llama1 = llama2;
            llama2 = null;
        } else if (merchant != null && merchant.isValid()) {
            llama1 = (TraderLlama) merchant.getWorld()
                    .spawnEntity(merchant.getLocation(), EntityType.TRADER_LLAMA);
            llama1.setLeashHolder(merchant);
            llama1.setGravity(true);
            llama1.setRemoveWhenFarAway(false);
            llama1.setAI(true);
        } else {
            llama1 = null;
        }
        return llama1;
    }

    public WanderingMerchantManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isActive() {
        return merchant != null && merchant.isValid();
    }

    public void spawnNear(Player player) {
        if (isActive()) return;
        Location base = player.getLocation().clone();
        base.add(player.getLocation().getDirection().multiply(-8));
        final Location centered = me.nakilex.levelplugin.lootchests.utils.LocationUtils.centerOnBlock(base);
        centered.getWorld().getChunkAtAsync(centered).thenRun(() ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    org.bukkit.Location ground = me.nakilex.levelplugin.lootchests.utils.LocationUtils.surfaceBelow(centered);
                    if (me.nakilex.levelplugin.lootchests.utils.LocationUtils.countAirAbove(ground) > 5) {
                        spawn(ground, player);
                    }
                })
        );
    }

    private void spawn(Location loc, Player player) {
        merchant = (WanderingTrader) loc.getWorld().spawnEntity(loc, EntityType.WANDERING_TRADER);
        merchant.setCustomName(ChatColor.GOLD + "Wandering Merchant");
        merchant.setCustomNameVisible(true);
        merchant.setAI(false);
        merchant.setRemoveWhenFarAway(false);
        merchant.setGravity(true);
        llama1 = (TraderLlama) loc.getWorld().spawnEntity(loc, EntityType.TRADER_LLAMA);
        llama2 = (TraderLlama) loc.getWorld().spawnEntity(loc, EntityType.TRADER_LLAMA);
        llama1.setLeashHolder(merchant);
        llama2.setLeashHolder(merchant);
        llama1.setGravity(true);
        llama2.setGravity(true);
        llama1.setAI(false);
        llama2.setAI(false);
        llama1.setRemoveWhenFarAway(false);
        llama2.setRemoveWhenFarAway(false);
        followTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) { cancel(); return; }
                org.bukkit.Location mLoc = merchant.getLocation();
                if (llama1 != null && llama1.isValid() && llama1.getLocation().distanceSquared(mLoc) > 25) {
                    llama1.teleport(mLoc);
                }
                if (llama2 != null && llama2.isValid() && llama2.getLocation().distanceSquared(mLoc) > 25) {
                    llama2.teleport(mLoc);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
        createShop(player);
        lastSpawn = System.currentTimeMillis();
    }

    private void createShop(Player basis) {
        List<WanderingMerchantOffer> offers = new ArrayList<>();
        List<CustomItem> items = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            int offerLevel = pickOfferLevel();
            CustomItem item = ItemManager.getInstance().generateItem("mob", offerLevel);
            items.add(item);
            ItemStack stack = ItemUtil.createItemStackFromCustomItem(item, 1, null);
            int gearScore = SalvageManager.getInstance().getTotalStats(item);
            int cost = gearScore * 2 + 5;
            offers.add(new WanderingMerchantOffer(stack, cost, 1));
        }
        gui = new WanderingMerchantGUI(plugin, offers);
        int totalGearScore = ItemUtil.calculateTotalGearScore(items);
        shopGearScore = totalGearScore;
        double maxHealth = totalGearScore * 2.0;
        if (merchant != null && merchant.getAttribute(Attribute.MAX_HEALTH) != null) {
            merchant.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
            merchant.setHealth(maxHealth);
        }
    }

    /** Choose a random item level with a bell-curve distribution. */
    private int pickOfferLevel() {
        int maxLevel = Main.getInstance().getLevelManager().getMaxLevel();
        double mean = maxLevel / 2.0;
        double stdDev = maxLevel / 6.0; // 99.7% within bounds
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int level;
        do {
            level = (int) Math.round(rand.nextGaussian() * stdDev + mean);
        } while (level < 1 || level > maxLevel);
        return level;
    }

    public void openShop(Player player) {
        if (gui != null) gui.open(player);
    }

    public void closeShop() {
        if (gui != null) gui.closeAll();
    }

    public void damage(Player attacker) {
        closeShop();
        startFlee(attacker);
    }

    public void recordHit() {
        lastDamage = System.currentTimeMillis();
    }

    /** Called when one of the companion llamas dies */
    public void handleLlamaDeath(TraderLlama llama) {
        if (llama == null) return;
        if (llama.equals(llama1)) {
            llama1 = null;
        }
        if (llama.equals(llama2)) {
            llama2 = null;
        }
    }

    public void startFlee(Player attacker) {
        if (merchant == null) return;
        ensureLlama();
        if (llama1 == null) return;
        merchant.setAI(true);
        merchant.setGravity(true);
        llama1.setAI(true);
        llama1.setGravity(true);
        llama1.setRemoveWhenFarAway(false);
        if (llama2 != null) {
            llama2.setAI(true);
            llama2.setRemoveWhenFarAway(false);
        }
        lastAttacker = attacker.getUniqueId();
        lastDamage = System.currentTimeMillis();

        // release leads so fleeing isn't constrained
        llama1.setLeashHolder(null);
        if (llama2 != null) {
            llama2.setLeashHolder(llama1);
        }

        // mount merchant on llama
        if (!llama1.getPassengers().contains(merchant)) {
            llama1.addPassenger(merchant);
        }

        // make llama fast
        if (llama1.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            llama1.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.45);
        }

        if (fleeTask != null) fleeTask.cancel();
        fleeTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) { cancel(); return; }
                if (System.currentTimeMillis() - lastDamage > 5_000L) {
                    Player p = lastAttacker != null ? Bukkit.getPlayer(lastAttacker) : null;
                    if (p != null) p.sendMessage(ChatColor.GRAY + "The wandering merchant has disappeared!");
                    despawn();
                    cancel();
                    return;
                }

                Player damager = Bukkit.getPlayer(lastAttacker);
                if (damager == null || !damager.isOnline()) return;

                org.bukkit.util.Vector dir = llama1.getLocation().toVector().subtract(damager.getLocation().toVector()).normalize();
                org.bukkit.util.Vector velocity = dir.multiply(0.45);
                double currentY = llama1.getVelocity().getY();
                velocity.setY(Math.min(currentY, 0.6));
                llama1.setVelocity(velocity);

                // teleport llama if too far from merchant (shouldn't happen when mounted)
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }


    public void despawn() {
        if (merchant != null) {
            merchant.remove();
            merchant = null;
        }
        if (llama1 != null) { llama1.remove(); llama1 = null; }
        if (llama2 != null) { llama2.remove(); llama2 = null; }
        if (gui != null) { gui.closeAll(); gui = null; }
        if (followTask != null) { followTask.cancel(); followTask = null; }
        if (fleeTask != null) { fleeTask.cancel(); fleeTask = null; }
    }

    public long getLastSpawn() { return lastSpawn; }

    public LivingEntity getMerchant() { return merchant; }
    public WanderingMerchantGUI getGui() { return gui; }
    public int getShopGearScore() { return shopGearScore; }
}
