package me.nakilex.levelplugin.horse.traits;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;

/**
 * Makes the horse temporarily invisible and non-collidable,
 * allowing it to slip past obstacles.
 */
public class GhostTrait implements HorseTrait {
    private static final String ID = "ghost";
    private static final int DURATION_TICKS = 20 * 5; // 5 seconds
    private static final int COOLDOWN_SECONDS = 45;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getCooldownSeconds() {
        return COOLDOWN_SECONDS;
    }

    @Override
    public void apply(Player player, AbstractHorse horse) {
        horse.setInvisible(true);
        horse.setCollidable(false);
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            horse.setInvisible(false);
            horse.setCollidable(true);
        }, DURATION_TICKS);
    }
}
