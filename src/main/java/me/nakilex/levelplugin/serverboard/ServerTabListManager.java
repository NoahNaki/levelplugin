package me.nakilex.levelplugin.serverboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Native replacement for TAB's {@code header-footer} section: the same branding header and
 * online/ping/staff footer, refreshed on a timer since the footer's ping is per-viewer.
 * <p>
 * Deliberately does NOT touch {@code Player#playerListName} - DeluxeTags already governs the
 * colored rank tag in front of names (visible in chat too), and setting it here would race with
 * that plugin over who wins each refresh. Only the header/footer text is ours to own.
 */
public final class ServerTabListManager implements Listener {
    private static final String STAFF_PERMISSION = "levelplugin.staffchat";
    private static final long REFRESH_INTERVAL_TICKS = 20L;

    private static final String HEADER =
            "\n" + net.md_5.bungee.api.ChatColor.of("#FF7080") + "" + ChatColor.BOLD + "Premium Prison" +
            "\n" + ChatColor.GRAY + "NitroSetups Development" + "\n";

    private final JavaPlugin plugin;
    private BukkitTask task;

    public ServerTabListManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 0L, REFRESH_INTERVAL_TICKS);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        update(event.getPlayer());
    }

    private void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player);
        }
    }

    private void update(Player player) {
        int online = Bukkit.getOnlinePlayers().size();
        long staff = Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission(STAFF_PERMISSION)).count();
        String footer = "\n" + "   " + ChatColor.WHITE + online + " " + net.md_5.bungee.api.ChatColor.of("#8CC1CD") + "Online "
                + ChatColor.GRAY + "◆ " + ChatColor.WHITE + player.getPing() + "ms " + net.md_5.bungee.api.ChatColor.of("#8CC1CD") + "Ping "
                + ChatColor.GRAY + "◆ " + ChatColor.WHITE + staff + " " + net.md_5.bungee.api.ChatColor.of("#8CC1CD") + "Staff   "
                + "\n\n" + ChatColor.GRAY + "" + ChatColor.ITALIC + "store.premiumprison.com"
                + "\n" + ChatColor.WHITE + "" + ChatColor.BOLD + "WEBSITE" + "\n";
        player.setPlayerListHeaderFooter(HEADER, footer);
    }
}
