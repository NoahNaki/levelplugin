package me.nakilex.levelplugin.cutscene.frames;

import me.nakilex.levelplugin.Main;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TeleportFrame implements Frame {
    private final Location location;
    private final String worldName;
    private final long durationMs;
    private final String title;
    private final String subtitle;
    private final String actionBar;
    private final String sound;
    private final String command;
    /** blocks per second, <=0 means instant */
    private final double speed;

    public TeleportFrame(Location location, long durationMs, String title, String subtitle,
                         String actionBar, String sound, String command, String worldName, double speed) {
        this.location = location;
        this.durationMs = durationMs;
        this.title = title;
        this.subtitle = subtitle;
        this.actionBar = actionBar;
        this.sound = sound;
        this.command = command;
        this.worldName = worldName;
        this.speed = speed;
    }

    @Override
    public long getDuration() {
        return durationMs;
    }

    public Location getLocation() {
        return location;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getSpeed() {
        return speed;
    }

    private static float wrapAngle(float angle) {
        angle = angle % 360f;
        if (angle >= 180f) angle -= 360f;
        if (angle < -180f) angle += 360f;
        return angle;
    }

    @Override
    public org.bukkit.scheduler.BukkitTask play(Player player, Main plugin) {
        if (location != null) {
            Location target = location.clone();
            if (worldName != null) {
                var world = plugin.getServer().getWorld(worldName);
                if (world != null) {
                    target.setWorld(world);
                }
            }

            if (speed > 0) {
                Location start = player.getLocation().clone();
                double distance = start.distance(target);
                double scaled = Math.pow(speed, 1.5); // amplify high speeds
                long ticks = Math.max(1L, Math.round(distance / scaled * 20.0));

                double dx = target.getX() - start.getX();
                double dy = target.getY() - start.getY();
                double dz = target.getZ() - start.getZ();
                float dyaw = wrapAngle(target.getYaw() - start.getYaw());
                float dpitch = target.getPitch() - start.getPitch();

                return new BukkitRunnable() {
                    long t = 0;
                    Location curr = start.clone();

                    @Override
                    public void run() {
                        if (t >= ticks) {
                            player.teleport(target);
                            playEffects(player);
                            cancel();
                            return;
                        }

                        double pct = smooth((t + 1) / (double) ticks);
                        curr.setX(start.getX() + dx * pct);
                        curr.setY(start.getY() + dy * pct);
                        curr.setZ(start.getZ() + dz * pct);
                        curr.setYaw(start.getYaw() + dyaw * (float) pct);
                        curr.setPitch(start.getPitch() + dpitch * (float) pct);
                        player.teleport(curr);
                        t++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            } else {
                player.teleport(target);
                playEffects(player);
                return null;
            }
        } else {
            playEffects(player);
            return null;
        }
        return null;
    }

    private double smooth(double t) {
        return 3 * t * t - 2 * t * t * t; // smoothstep
    }

    private void playEffects(Player player) {
        if (title != null || subtitle != null) {
            String t = title == null ? "" : ChatColor.translateAlternateColorCodes('&', title);
            String sub = subtitle == null ? "" : ChatColor.translateAlternateColorCodes('&', subtitle);
            player.sendTitle(t, sub, 10, 40, 10);
        }
        if (actionBar != null) {
            String msg = ChatColor.translateAlternateColorCodes('&', actionBar);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
        }
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1f, 1f);
        }
        if (command != null && !command.isEmpty()) {
            String cmd = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }
}
