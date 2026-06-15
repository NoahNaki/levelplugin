package me.nakilex.levelplugin.cooking.service;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** Owns vanilla Bukkit sound and particle feedback for cooking gameplay. */
public class CookingEffectsService {
    public void playIngredientInserted(Player player, Location workstation) {
        Location center = center(workstation, player);
        playSound(player, center, Sound.ENTITY_ITEM_PICKUP, 0.7F, 1.4F);
        spawn(center, Particle.HAPPY_VILLAGER, 5, 0.22D, 0.18D, 0.22D, 0.01D);
    }

    public void playWrongIngredient(Player player, Location workstation) {
        Location center = center(workstation, player);
        playSound(player, center, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7F, 0.7F);
        spawn(center, Particle.SMOKE, 6, 0.18D, 0.12D, 0.18D, 0.01D);
    }

    public void playMiniGameGoodClick(Player player, Location workstation) {
        Location center = center(workstation, player);
        playSound(player, center, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8F, 1.6F);
        spawn(center, Particle.CRIT, 8, 0.25D, 0.18D, 0.25D, 0.02D);
    }

    public void playMiniGameGoodClick(Player player, Location workstation, double progressRatio) {
        Location center = center(workstation, player);
        float pitch = (float) (0.8D + Math.max(0.0D, Math.min(1.0D, progressRatio)) * 0.8D);
        playSound(player, center, Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 0.7F, pitch);
        if (progressRatio < 0.33D) {
            spawn(center, Particle.SMOKE, 4, 0.16D, 0.1D, 0.16D, 0.005D);
        } else if (progressRatio < 0.66D) {
            spawn(center, Particle.FLAME, 4, 0.16D, 0.1D, 0.16D, 0.01D);
        } else {
            spawn(center, Particle.FLAME, 6, 0.18D, 0.12D, 0.18D, 0.01D);
            spawn(center, Particle.LAVA, 2, 0.12D, 0.05D, 0.12D, 0.0D);
        }
    }

    public void playMiniGameBadClick(Player player, Location workstation) {
        Location center = center(workstation, player);
        playSound(player, center, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8F, 0.6F);
        spawn(center, Particle.SMOKE, 8, 0.25D, 0.16D, 0.25D, 0.01D);
    }

    public void playMiniGameSuccess(Player player, Location workstation) {
        Location center = center(workstation, player);
        playSound(player, center, Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.3F);
        spawn(center, Particle.FLAME, 10, 0.25D, 0.22D, 0.25D, 0.015D);
        spawn(center, Particle.HAPPY_VILLAGER, 8, 0.25D, 0.22D, 0.25D, 0.01D);
    }

    public void playMiniGameFail(Player player, Location workstation) {
        Location center = center(workstation, player);
        playSound(player, center, Sound.ENTITY_ITEM_BREAK, 0.8F, 0.8F);
        spawn(center, Particle.SMOKE, 12, 0.28D, 0.2D, 0.28D, 0.02D);
    }

    public void playWaitTick(Player player, Location workstation) {
        Location center = center(workstation, player);
        playSound(player, center, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.35F, 1.0F);
        spawn(center, Particle.SMOKE, 3, 0.16D, 0.1D, 0.16D, 0.005D);
        spawn(center, Particle.FLAME, 2, 0.12D, 0.08D, 0.12D, 0.005D);
    }

    public void playCookingComplete(Player player, Location workstation) {
        Location center = center(workstation, player);
        playSound(player, center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.3F);
        playSound(player, center, Sound.BLOCK_FIRE_EXTINGUISH, 0.6F, 1.4F);
        spawn(center, Particle.SMOKE, 16, 0.3D, 0.28D, 0.3D, 0.01D);
        spawn(center, Particle.FLAME, 14, 0.25D, 0.24D, 0.25D, 0.015D);
        spawn(center.clone().add(0.0D, 0.3D, 0.0D), Particle.HAPPY_VILLAGER, 8, 0.25D, 0.25D, 0.25D, 0.01D);
    }

    private Location center(Location workstation, Player player) {
        if (workstation != null && workstation.getWorld() != null) {
            return workstation.clone().add(0.5D, 1.0D, 0.5D);
        }
        return player == null ? null : player.getLocation();
    }

    private void playSound(Player player, Location location, Sound sound, float volume, float pitch) {
        if (location == null || location.getWorld() == null || sound == null) {
            return;
        }
        location.getWorld().playSound(location, sound, volume, pitch);
        if (player != null && player.isOnline() && player.getWorld() != location.getWorld()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    private void spawn(Location location, Particle particle, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        if (location == null || location.getWorld() == null || particle == null) {
            return;
        }
        World world = location.getWorld();
        world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
    }
}
