package me.nakilex.levelplugin.dungeon.ai;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.Dungeon;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.dungeon.DungeonMobSpawnListener;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.lootchests.data.ChestData;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.potions.data.PotionInstance;
import me.nakilex.levelplugin.salvage.managers.SalvageManager;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.ArrayList;

/**
 * Simple controller that moves an NPC along the path returned by
 * {@link DungeonPathfinder} and triggers dungeon mob spawns while moving.
 */
public class DungeonNPCRunner extends BukkitRunnable {
    private final NPC npc;
    private final List<Location> route;
    private final Dungeon dungeon;
    private final DungeonManager manager;
    private final LootChestManager lootChestManager;
    private final DungeonMobSpawnListener spawnListener;
    private final List<Location> chestLocations = new ArrayList<>();
    private final List<org.bukkit.inventory.ItemStack> lootedItems = new ArrayList<>();
    private int lootedCoins = 0;
    private final Player owner;
    private boolean finished = false;
    private final EconomyManager economy;

    private static final java.util.Map<Integer, DungeonNPCRunner> RUNNERS = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, DungeonNPCRunner> BY_OWNER = new java.util.HashMap<>();
    private LivingEntity hostileTarget;
    private Location chestTarget;
    private int index = 0;
    private Location last;

    public DungeonNPCRunner(NPC npc, Dungeon dungeon, DungeonManager manager,
                            DungeonMobSpawnListener listener, Player owner) {
        this.npc = npc;
        this.dungeon = dungeon;
        this.manager = manager;
        this.lootChestManager = manager.getLootChestManager();
        this.spawnListener = listener;
        this.owner = owner;
        this.economy = Main.getInstance().getEconomyManager();
        this.route = DungeonPathfinder.computePath(dungeon, manager);
        for (Dungeon.RoomInstance r : dungeon.getRooms()) {
            chestLocations.addAll(r.chests);
        }
    }

    public void start(Main plugin) {
        if (route.isEmpty()) return;
        npc.getNavigator().getDefaultParameters().speedModifier(1.5f);
        npc.getNavigator().getLocalParameters().speedModifier(1.5f);
        npc.getNavigator().setTarget(route.get(0));
        RUNNERS.put(npc.getId(), this);
        if (owner != null) BY_OWNER.put(owner.getUniqueId(), this);
        this.runTaskTimer(plugin, 20L, 10L);
    }

    @Override
    public void run() {
        if (index >= route.size()) {
            finish();
            return;
        }
        Location current = npc.getEntity().getLocation();
        if (last != null) spawnListener.handleMove(npc.getEntity(), last, current);
        last = current;
        // ---- Hostile mobs ----
        if (hostileTarget != null) {
            if (!hostileTarget.isValid() || hostileTarget.isDead() ||
                    hostileTarget.getLocation().distanceSquared(current) > 400) {
                hostileTarget = null;
            } else {
                npc.getNavigator().setTarget(hostileTarget, true);
                return;
            }
        }
        if (hostileTarget == null) {
            hostileTarget = findNearestHostile(current);
            if (hostileTarget != null) {
                npc.getNavigator().setTarget(hostileTarget, true);
                return;
            }
        }

        // ---- Nearby chests ----
        if (chestTarget != null) {
            if (current.distanceSquared(chestTarget) < 4) {
                lootChest(chestTarget);
                chestLocations.remove(chestTarget);
                chestTarget = null;
            } else {
                if (!npc.getNavigator().isNavigating()) {
                    npc.getNavigator().setTarget(chestTarget);
                }
                return;
            }
        }
        if (chestTarget == null) {
            chestTarget = findNearbyChest(current);
            if (chestTarget != null) {
                npc.getNavigator().setTarget(chestTarget);
                return;
            }
        }

        // ---- Follow path ----
        Location target = route.get(index);
        if (!npc.getNavigator().isNavigating()) {
            npc.getNavigator().setTarget(target);
        }
        if (current.distanceSquared(target) < 1.5) {
            index++;
            if (index < route.size()) {
                npc.getNavigator().setTarget(route.get(index));
            }
        }
    }

    private LivingEntity findNearestHostile(Location loc) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 20, 20, 20)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (le instanceof Player) continue;
            if (CitizensAPI.getNPCRegistry().isNPC(le)) continue;
            if (le.isDead()) continue;
            double d = le.getLocation().distanceSquared(loc);
            if (d < bestDist) { bestDist = d; best = le; }
        }
        return best;
    }

    private Location findNearbyChest(Location loc) {
        Location best = null;
        double bestDist = Double.MAX_VALUE;
        for (Location c : new ArrayList<>(chestLocations)) {
            Integer id = lootChestManager.getChestIdAtLocation(c);
            if (id == null) { chestLocations.remove(c); continue; }
            double d = c.distanceSquared(loc);
            if (d <= 100 && d < bestDist) { bestDist = d; best = c; }
        }
        return best;
    }

    private void lootChest(Location loc) {
        Integer id = lootChestManager.getChestIdAtLocation(loc);
        if (id == null) return;
        if (lootChestManager.isChestViewed(id)) return;
        ChestData data = lootChestManager.getChestDataById(id);
        if (data != null) {
            org.bukkit.inventory.ItemStack loot = data.getBufferedLootItem();
            if (loot != null && owner != null) {
                lootedItems.add(loot.clone());
            }
            int tier = data.getTier();
            lootedCoins += rollCoins(tier);
        }
        lootChestManager.removeChest(id);
    }

    private int rollCoins(int tier) {
        if (Math.random() >= 0.4) return 0;
        int min, max;
        if (tier <= 2) { min = 10; max = 20; }
        else if (tier <= 4) { min = 25; max = 40; }
        else { min = 50; max = 75; }
        return new java.util.Random().nextInt(max - min + 1) + min;
    }

    private void finish() {
        finished = true;
        this.cancel();
        npc.getNavigator().cancelNavigation();
        if (owner == null) {
            RUNNERS.remove(npc.getId());
            npc.despawn();
        }
    }

    public static DungeonNPCRunner getRunner(NPC npc) { return RUNNERS.get(npc.getId()); }
    public static DungeonNPCRunner getRunner(java.util.UUID owner) { return BY_OWNER.get(owner); }

    public void openLootGUI(Player player) {
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 27, org.bukkit.ChatColor.DARK_GREEN + "NPC Loot");
        for (int i = 0; i < lootedItems.size() && i < 27; i++) {
            inv.setItem(i, lootedItems.get(i));
        }
        player.openInventory(inv);
        org.bukkit.Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
                if (!e.getInventory().equals(inv)) return;
                lootedItems.clear();
                for (org.bukkit.inventory.ItemStack it : e.getInventory().getContents()) {
                    if (it == null || it.getType().isAir()) continue;
                    lootedItems.add(it);
                }
                org.bukkit.event.HandlerList.unregisterAll(this);
            }
            @org.bukkit.event.EventHandler
            public void onClick(org.bukkit.event.inventory.InventoryClickEvent e) {
                if (e.getInventory().equals(inv)) {
                    // allow taking items
                }
            }
        }, Main.getInstance());
    }

    public void onPlayerLeave(Player player) {
        if (owner == null || !owner.getUniqueId().equals(player.getUniqueId())) return;
        int total = lootedCoins;
        for (org.bukkit.inventory.ItemStack it : lootedItems) {
            total += computePrice(it);
        }
        if (total > 0) {
            economy.addCoins(player, total);
            player.sendMessage(org.bukkit.ChatColor.GOLD + "You received " + org.bukkit.ChatColor.YELLOW + total + " coins from your mercenary.");
        }
        lootedItems.clear();
        lootedCoins = 0;
        RUNNERS.remove(npc.getId());
        BY_OWNER.remove(player.getUniqueId());
        npc.despawn();
    }

    private int computePrice(org.bukkit.inventory.ItemStack stack) {
        CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(stack);
        if (ci != null) return SalvageManager.getInstance().getSellPrice(ci);
        PotionInstance pi = Main.getInstance().getPotionManager().getInstanceFromItem(stack);
        if (pi != null) return SalvageManager.getInstance().getPotionSellPrice(pi);
        return 0;
    }
}
