package me.nakilex.levelplugin.screen.menu;

import me.nakilex.levelplugin.screen.util.ColorParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One clickable button on the screen. The layout contains the coordinates of
 * the button relative to the camera as well as actions to execute when the
 * player clicks it.
 */
public class MenuLayout {
    private final double x;
    private final double y;
    private final double z;
    private final float tilt;
    private final List<String> commands;
    private final boolean stop;
    private final Location teleport;
    private final String permission;

    private final ConcurrentHashMap<UUID, Long> cooldown = new ConcurrentHashMap<>();

    public MenuLayout(double x, double y, double z, float tilt, List<String> commands,
                      boolean stop, @Nullable Location teleport, @Nullable String permission) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.tilt = tilt;
        this.commands = commands == null ? Collections.emptyList() : commands;
        this.stop = stop;
        this.teleport = teleport;
        this.permission = permission;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getTilt() { return tilt; }
    public boolean shouldStop() { return stop; }

    public boolean hasPermission(Player player) {
        if (permission == null || permission.isEmpty()) return true;
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            if (pai.getPermission().equalsIgnoreCase(permission) && pai.getValue()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Execute all configured commands. Placeholders are expanded using a very
     * small subset to avoid a hard dependency on PlaceholderAPI.
     */
    public void runCommands(Player player) {
        for (String raw : commands) {
            String cmd = raw.replace("%player%", player.getName());
            cmd = ColorParser.parse(cmd);
            CommandSender sender = player;
            if (cmd.startsWith("[console]")) {
                sender = Bukkit.getConsoleSender();
                cmd = cmd.substring("[console]".length());
            } else if (cmd.startsWith("[op]")) {
                boolean wasOp = player.isOp();
                try {
                    player.setOp(true);
                    Bukkit.dispatchCommand(player, cmd.substring("[op]".length()));
                } finally {
                    player.setOp(wasOp);
                }
                continue;
            }
            Bukkit.dispatchCommand(sender, cmd);
        }
    }

    public void teleport(Player player) {
        if (teleport != null) {
            player.teleport(teleport);
        }
    }

    public boolean isClick(Player player, long cooldownMillis) {
        long now = System.currentTimeMillis();
        Long last = cooldown.put(player.getUniqueId(), now);
        return last != null && (now - last) < cooldownMillis;
    }
}
