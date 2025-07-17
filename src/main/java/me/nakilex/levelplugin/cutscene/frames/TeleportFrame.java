package me.nakilex.levelplugin.cutscene.frames;

import me.nakilex.levelplugin.Main;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TeleportFrame implements Frame {
    private final Location location;
    private final long durationMs;
    private final String title;
    private final String subtitle;
    private final String actionBar;
    private final String sound;
    private final String command;

    public TeleportFrame(Location location, long durationMs, String title, String subtitle,
                         String actionBar, String sound, String command) {
        this.location = location;
        this.durationMs = durationMs;
        this.title = title;
        this.subtitle = subtitle;
        this.actionBar = actionBar;
        this.sound = sound;
        this.command = command;
    }

    @Override
    public long getDuration() {
        return durationMs;
    }

    @Override
    public void play(Player player, Main plugin) {
        if (location != null) {
            player.teleport(location);
        }
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
