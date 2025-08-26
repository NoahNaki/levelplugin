package me.nakilex.levelplugin.maintenance;

import me.nakilex.levelplugin.Main;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class MaintenanceManager {
    private final Main plugin;
    private boolean enabled = false;
    private String reason = "";

    public MaintenanceManager(Main plugin) {
        this.plugin = plugin;
    }

    public void enable(String reason) {
        this.enabled = true;
        this.reason = reason == null ? "" : reason;
        plugin.getServer().setWhitelist(true);
    }

    public void disable() {
        this.enabled = false;
        this.reason = "";
        plugin.getServer().setWhitelist(false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getReason() {
        return reason;
    }

    public void addPlayer(String name) {
        OfflinePlayer player = plugin.getServer().getOfflinePlayer(name);
        player.setWhitelisted(true);
    }

    public void removePlayer(String name) {
        OfflinePlayer player = plugin.getServer().getOfflinePlayer(name);
        player.setWhitelisted(false);
    }

    public Set<String> getWhitelist() {
        Server server = plugin.getServer();
        return server.getWhitelistedPlayers().stream()
                .map(OfflinePlayer::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
