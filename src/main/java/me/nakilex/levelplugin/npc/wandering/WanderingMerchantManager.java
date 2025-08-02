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
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Spawns a wandering merchant NPC with a llama companion.
 * Generates a random shop inventory when spawned.
 */
public class WanderingMerchantManager {
    private final Main plugin;
    private Villager merchant;
    private TraderLlama llama1;
    private TraderLlama llama2;
    private WanderingMerchantGUI gui;
    private long lastSpawn = 0L;
    private long lastDamage = 0L;
    private java.util.UUID lastAttacker;
    private org.bukkit.scheduler.BukkitTask fleeTask;
    private org.bukkit.scheduler.BukkitTask followTask;
    private TraderLlama ensureLlama() {
        if (llama1 != null && llama1.isValid()) return llama1;
        // attempt to reuse second llama if alive
        if (llama2 != null && llama2.isValid()) {
            llama1 = llama2;
            llama2 = null;
        } else if (merchant != null && merchant.isValid()) {
            llama1 = (TraderLlama) merchant.getWorld()
                    .spawnEntity(merchant.getLocation(), EntityType.TRADER_LLAMA);
            llama1.setLeashHolder(merchant);
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
        final Location spawnLoc = me.nakilex.levelplugin.lootchests.utils.LocationUtils.aboveSurface(base);
        spawnLoc.getWorld().getChunkAtAsync(spawnLoc).thenRun(() ->
                Bukkit.getScheduler().runTask(plugin, () -> spawn(spawnLoc, player))
        );
    }

    private void spawn(Location loc, Player player) {
        loc = me.nakilex.levelplugin.lootchests.utils.LocationUtils.aboveSurface(loc);
        merchant = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        merchant.setCustomName(ChatColor.GOLD + "Wandering Merchant");
        merchant.setCustomNameVisible(true);
        merchant.setAI(false);
        merchant.setRemoveWhenFarAway(false);
        llama1 = (TraderLlama) loc.getWorld().spawnEntity(loc, EntityType.TRADER_LLAMA);
        llama2 = (TraderLlama) loc.getWorld().spawnEntity(loc, EntityType.TRADER_LLAMA);
        llama1.setLeashHolder(merchant);
        llama2.setLeashHolder(merchant);
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
        int playerLevel = Main.getInstance().getLevelManager().getLevel(basis);
        for (int i = 0; i < 7; i++) {
            int offerLevel = pickOfferLevel(playerLevel);
            CustomItem item = ItemManager.getInstance().generateItem("mob", offerLevel);
            ItemStack stack = ItemUtil.createItemStackFromCustomItem(item, 1, null);
            int cost = SalvageManager.getInstance().getTotalStats(item) * 2 + 5;
            offers.add(new WanderingMerchantOffer(stack, cost, 1));
        }
        gui = new WanderingMerchantGUI(plugin, offers);
    }

    /** Choose a random item level near the player's level. */
    private int pickOfferLevel(int baseLevel) {
        int delta = ThreadLocalRandom.current().nextInt(-2, 3); // [-2, +2]
        return Math.max(1, baseLevel + delta);
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
        lastAttacker = attacker.getUniqueId();
        lastDamage = System.currentTimeMillis();

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
}
