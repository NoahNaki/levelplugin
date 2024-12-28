package me.nakilex.levelplugin.horse.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.horse.data.HorseData;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;

import java.util.HashMap;
import java.util.UUID;

public class HorseManager {

    private final HorseConfigManager configManager;
    private final HashMap<UUID, HorseData> horses = new HashMap<>();

    // Constructor to accept HorseConfigManager
    public HorseManager(HorseConfigManager configManager) {
        this.configManager = configManager;
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
        HorseData horseData = getHorse(player.getUniqueId());
        if (horseData == null) {
            player.sendMessage("You do not own a horse.");
            return;
        }

        // Define variables for horse spawning
        org.bukkit.entity.AbstractHorse horse;

        // Handle special variants
        switch (horseData.getType().toUpperCase()) {
            case "ZOMBIE":
                horse = player.getWorld().spawn(player.getLocation(), org.bukkit.entity.ZombieHorse.class);
                break;
            case "SKELETON":
                horse = player.getWorld().spawn(player.getLocation(), org.bukkit.entity.SkeletonHorse.class);
                break;
            default: // Handle standard horse colors
                Horse normalHorse = player.getWorld().spawn(player.getLocation(), Horse.class);
                normalHorse.setColor(Horse.Color.valueOf(horseData.getType().toUpperCase()));
                horse = normalHorse;
                break;
        }

        // Set common properties for all horses
        horse.setOwner(player);
        horse.setTamed(true);
        horse.setCustomName(player.getName() + "'s Horse");
        horse.setCustomNameVisible(true);

        // Set jump and speed attributes
        horse.setJumpStrength(horseData.getJumpHeight() / 10.0);
        horse.getAttribute(Attribute.MOVEMENT_SPEED)
            .setBaseValue(horseData.getSpeed() / 10.0);

        // Equip saddle for all types
        if (horse instanceof Horse) { // Normal horses
            ((Horse) horse).getInventory().setSaddle(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SADDLE));
        } else if (horse instanceof org.bukkit.entity.ZombieHorse || horse instanceof org.bukkit.entity.SkeletonHorse) {
            // Apply saddle appearance and enable riding control
            horse.getInventory().setSaddle(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SADDLE));
        }

        // Mount the player
        horse.addPassenger(player);

        // Handle dismount to despawn the horse
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onDismount(EntityDismountEvent event) {
                    if (event.getEntity() instanceof Player && event.getDismounted().equals(horse)) {
                        horse.remove(); // Despawn the horse
                        HandlerList.unregisterAll(this); // Cleanup listener
                    }
                }
            }, Main.getInstance());
        });
    }

    public void dismountHorse(Player player) {
        // Check if the player is riding a horse
        if (player.isInsideVehicle() && player.getVehicle() instanceof org.bukkit.entity.AbstractHorse) {
            org.bukkit.entity.AbstractHorse horse = (org.bukkit.entity.AbstractHorse) player.getVehicle();
            horse.remove(); // Despawn the horse
            player.leaveVehicle(); // Ensure the player is removed from the horse
        }
    }

}
