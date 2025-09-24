package me.nakilex.levelplugin.cutscene.frames;

import me.nakilex.levelplugin.cutscene.playback.CutsceneContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class BranchFrame implements Frame {
    private final String permission;
    private final boolean invert;
    private final String message;
    private final long durationMs;

    public BranchFrame(String permission, boolean invert, String message, long durationMs) {
        this.permission = permission;
        this.invert = invert;
        this.message = message;
        this.durationMs = durationMs;
    }

    @Override
    public long getDuration() {
        return durationMs;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isInvert() {
        return invert;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public org.bukkit.scheduler.BukkitTask play(CutsceneContext context) {
        Player viewer = context.getViewer();
        boolean allowed = permission == null || viewer.hasPermission(permission);
        if (invert) {
            allowed = !allowed;
        }
        if (allowed) {
            if (message != null && !message.isEmpty()) {
                viewer.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }
            Bukkit.getScheduler().runTask(context.getPlugin(), () -> context.getPlayback().skip(viewer));
        }
        return null;
    }
}
