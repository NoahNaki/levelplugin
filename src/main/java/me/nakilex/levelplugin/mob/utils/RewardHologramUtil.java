package me.nakilex.levelplugin.mob.utils;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Utility for showing short lived reward holograms when a mob dies.
 */
public final class RewardHologramUtil {

    private RewardHologramUtil() {}

    /**
     * Display a two-line hologram showing awarded XP and coins.
     *
     * @param loc   base world location
     * @param xp    experience amount
     * @param coins coins amount
     */
    public static void showRewardHologram(Location loc, int xp, int coins) {
        loc = loc.clone().add(0, 1.2, 0);
        String amountColor = ChatFormatter.experienceColor();
        String xpLine = ChatColor.GRAY + "[" + amountColor + "+" + xp + " "
                + ChatColor.GREEN + "<glyph:experience_orb_icon> "
                + ChatFormatter.experienceLabel() + ChatColor.GRAY + "]";
        ArmorStand xpStand = loc.getWorld().spawn(loc, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setMarker(true);
            as.setCustomNameVisible(true);
            as.setCustomName(xpLine);
        });
        String coinLine = ChatColor.GRAY + "[" + amountColor + "+" + coins + " "
                + ChatColor.GOLD + "<glyph:coins_icon>" + ChatColor.GRAY + "]";
        Location coinLoc = loc.clone().add(0, -0.3, 0);
        ArmorStand coinStand = coinLoc.getWorld().spawn(coinLoc, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setMarker(true);
            as.setCustomNameVisible(true);
            as.setCustomName(coinLine);
        });
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!xpStand.isDead()) xpStand.remove();
                if (!coinStand.isDead()) coinStand.remove();
            }
        }.runTaskLater(Main.getInstance(), 40L);
    }
}
