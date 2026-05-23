package me.nakilex.levelplugin.pet.listeners;

import me.nakilex.levelplugin.lootchests.listeners.LootChestListener;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.pet.PetEffectType;
import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Applies non-combat utility pet effects such as chest proximity opening and item magnet pull.
 */
public class PetUtilityEffectListener implements Listener {
    private static final long CHEST_OPEN_COOLDOWN_MS = 1_500L;
    private static final long ITEM_PULL_COOLDOWN_MS = 250L;
    private static final double ITEM_PULL_MIN_DISTANCE = 0.6;

    private final PetManager petManager;
    private final LootChestListener lootChestListener;
    private final LootChestManager lootChestManager;
    private final Map<UUID, Long> nextChestOpenAt = new HashMap<>();
    private final Map<UUID, Long> nextItemPullAt = new HashMap<>();

    public PetUtilityEffectListener(PetManager petManager,
                                    LootChestListener lootChestListener,
                                    LootChestManager lootChestManager) {
        this.petManager = petManager;
        this.lootChestListener = lootChestListener;
        this.lootChestManager = lootChestManager;

        Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online == null || !online.isOnline()) continue;
                handleItemMagnet(online);
            }
        }, 10L, 10L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        if (event.getFrom() != null && event.getTo() != null
                && event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        applyUtilityEffects(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        applyUtilityEffects(event.getPlayer());
    }

    private void applyUtilityEffects(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        handleChestProximityOpen(player);
        handleItemMagnet(player);
    }

    private void handleChestProximityOpen(Player player) {
        if (lootChestListener == null || lootChestManager == null) {
            return;
        }
        double range = petManager.getActiveEffectValue(player.getUniqueId(), PetEffectType.CHEST_PROXIMITY_OPEN);
        if (range <= 0.0) {
            return;
        }
        long now = System.currentTimeMillis();
        long nextAllowed = nextChestOpenAt.getOrDefault(player.getUniqueId(), 0L);
        if (now < nextAllowed) {
            return;
        }

        Integer nearestId = lootChestManager.findNearestChestId(player.getLocation());
        if (nearestId == null) {
            return;
        }
        Location chestLoc = lootChestManager.getLocationForChestId(nearestId);
        if (chestLoc == null || chestLoc.getWorld() == null || !chestLoc.getWorld().equals(player.getWorld())) {
            return;
        }
        if (chestLoc.distanceSquared(player.getLocation()) > range * range) {
            return;
        }
        if (lootChestListener.openLootChest(player, chestLoc)) {
            nextChestOpenAt.put(player.getUniqueId(), now + CHEST_OPEN_COOLDOWN_MS);
        }
    }

    private void handleItemMagnet(Player player) {
        double range = petManager.getActiveEffectValue(player.getUniqueId(), PetEffectType.ITEM_MAGNET);
        if (range <= 0.0) {
            return;
        }
        long now = System.currentTimeMillis();
        long nextAllowed = nextItemPullAt.getOrDefault(player.getUniqueId(), 0L);
        if (now < nextAllowed) {
            return;
        }
        nextItemPullAt.put(player.getUniqueId(), now + ITEM_PULL_COOLDOWN_MS);

        for (Entity nearby : player.getNearbyEntities(range, range, range)) {
            if (!(nearby instanceof Item item)) {
                continue;
            }
            if (!item.isValid() || item.isDead()) {
                continue;
            }
            Vector delta = player.getLocation().toVector().subtract(item.getLocation().toVector());
            double distance = delta.length();
            if (distance <= ITEM_PULL_MIN_DISTANCE) {
                continue;
            }
            Vector velocity = delta.normalize().multiply(0.35).setY(0.15);
            item.setVelocity(velocity);
        }
    }
}
