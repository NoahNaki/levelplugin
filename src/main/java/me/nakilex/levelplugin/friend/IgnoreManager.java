package me.nakilex.levelplugin.friend;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;

/**
 * Manages player ignore lists and hides ignored players.
 */
public class IgnoreManager implements Listener {
    private final Main plugin;
    private final Map<UUID, Set<UUID>> ignores = new HashMap<>();

    public IgnoreManager(Main plugin) {
        this.plugin = plugin;
    }

    private Set<UUID> getList(UUID id) {
        return ignores.computeIfAbsent(id, k -> new HashSet<>());
    }

    public boolean ignore(UUID player, UUID target) {
        if (player.equals(target)) return false;
        return getList(player).add(target);
    }

    public boolean unignore(UUID player, UUID target) {
        return getList(player).remove(target);
    }

    public boolean isIgnoring(UUID player, UUID target) {
        return getList(player).contains(target);
    }

    public Set<UUID> getIgnored(UUID player) {
        return Collections.unmodifiableSet(getList(player));
    }

    /** Hide or show ignored players for this viewer. */
    public void apply(Player viewer) {
        Set<UUID> set = getList(viewer.getUniqueId());
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(viewer)) continue;
            if (set.contains(p.getUniqueId())) viewer.hidePlayer(plugin, p);
            else viewer.showPlayer(plugin, p);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();
        // hide ignored players from the joiner
        Bukkit.getScheduler().runTaskLater(plugin, () -> apply(joined), 1L);
        // hide this joiner from others who ignore them
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(joined)) continue;
            if (isIgnoring(p.getUniqueId(), joined.getUniqueId())) {
                p.hidePlayer(plugin, joined);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        UUID sender = event.getPlayer().getUniqueId();
        event.getRecipients().removeIf(p -> isIgnoring(p.getUniqueId(), sender));
    }
}
