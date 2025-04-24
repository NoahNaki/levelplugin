package me.nakilex.levelplugin.settings.managers;

import me.nakilex.levelplugin.settings.data.PlayerSettings;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class SettingsManager {

    private final HashMap<UUID, PlayerSettings> settingsMap = new HashMap<>();

    public PlayerSettings getSettings(Player player) {
        return settingsMap.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerSettings());
    }
}
