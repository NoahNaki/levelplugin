package me.nakilex.levelplugin.horse.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.horse.data.HorseData;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class HorseManager implements Listener {

    private final HorseConfigManager configManager;
    private final HashMap<UUID, HorseData> horses = new HashMap<>();
    private final HashMap<UUID, Long> lastSpawnTimestamps = new HashMap<>();
    private static final long COOLDOWN_MS = 5_000L; // 5 seconds

    // Constructor to accept HorseConfigManager
    public HorseManager(HorseConfigManager configManager) {
        this.configManager = configManager;
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());

        // Load all previously saved horses into memory
        Set<String> keys = configManager.getHorseUUIDStrings();
        for (String uuidStr : keys) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                HorseData data = configManager.loadHorseData(uuid);
                if (data != null) {
                    horses.put(uuid, data);
                }
            } catch (IllegalArgumentException ignored) {
                // Skip invalid UUID entries
            }
        }
    }

    // Example methods
    public HorseData getHorse(UUID uuid) {
        return horses.get(uuid);
    }

    public void rerollHorse(UUID uuid) {
        HorseData newHorse = HorseData.randomHorse(uuid);
        horses.put(uuid, newHorse);
        configManager.saveHorseData(uuid, newHorse); // Persist data
    }

    public void spawnHorse(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // ── Cooldown check ───────────────────────────────────────────────────────────
        Long last = lastSpawnTimestamps.get(uuid);
        if (last != null && now - last < COOLDOWN_MS) {
            long secsLeft = (COOLDOWN_MS - (now - last) + 999) / 1000;
            player.sendMessage(ChatColor.RED +
                "Please wait " + secsLeft + " more second" +
                (secsLeft == 1 ? "" : "s") + " before spawning another horse.");
            return;
        }

        // record this spawn time
        lastSpawnTimestamps.put(uuid, now);

        // ── Existing spawn logic ──────────────────────────────────────────────────────
        HorseData horseData = getHorse(uuid);
        if (horseData == null) {
            player.sendMessage(ChatColor.RED + "You do not own a horse.");
            return;
        }

        AbstractHorse horse;
        switch (horseData.getType().toUpperCase()) {
            case "ZOMBIE":
                horse = player.getWorld().spawn(player.getLocation(), org.bukkit.entity.ZombieHorse.class);
                break;
            case "SKELETON":
                horse = player.getWorld().spawn(player.getLocation(), org.bukkit.entity.SkeletonHorse.class);
                break;
            default:
                Horse normalHorse = player.getWorld().spawn(player.getLocation(), Horse.class);
                normalHorse.setColor(Horse.Color.valueOf(horseData.getType().toUpperCase()));
                horse = normalHorse;
                break;
        }

        horse.setOwner(player);
        horse.setTamed(true);
        horse.setCustomName(player.getName() + "'s Horse");
        horse.setCustomNameVisible(true);
        horse.setInvulnerable(true);
        horse.setJumpStrength(horseData.getJumpHeight() / 10.0);
        horse.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(horseData.getSpeed() / 10.0);
        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        horse.addPassenger(player);

        // despawn on dismount
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onDismount(EntityDismountEvent event) {
                    if (event.getEntity() instanceof Player
                        && event.getDismounted().equals(horse)) {
                        horse.remove();
                        HandlerList.unregisterAll(this);
                    }
                }
            }, Main.getInstance());
        });
    }

    public void dismountHorse(Player player) {
        // Check if the player is riding a horse
        if (player.isInsideVehicle() && player.getVehicle() instanceof AbstractHorse) {
            AbstractHorse horse = (AbstractHorse) player.getVehicle();
            horse.remove(); // Despawn the horse
            player.leaveVehicle(); // Ensure the player is removed from the horse
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Prevent removing saddle from horse inventory
        if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof AbstractHorse) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() == Material.SADDLE) {
                event.setCancelled(true);
            }
        }
    }
}
