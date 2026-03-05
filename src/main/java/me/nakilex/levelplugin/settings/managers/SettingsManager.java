package me.nakilex.levelplugin.settings.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.player.profile.ProfileManager;
import me.nakilex.levelplugin.settings.data.PlayerSettings;
import me.nakilex.levelplugin.spells.input.SpellInputMode;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class SettingsManager {

    private final HashMap<UUID, PlayerSettings> settingsMap = new HashMap<>();

    public PlayerSettings getSettings(Player player) {
        return getSettings(player.getUniqueId());
    }

    public PlayerSettings getSettings(UUID id) {
        return settingsMap.computeIfAbsent(id, uuid -> new PlayerSettings());
    }

    public void saveActiveProfileSettings(Player player) {
        Integer slot = ProfileManager.getInstance().getActiveSlot(player.getUniqueId());
        if (slot == null || slot < 0) {
            return;
        }
        saveProfileSettings(player.getUniqueId(), slot);
    }

    public void saveProfileSettings(UUID playerId, int slot) {
        if (playerId == null || slot < 0) {
            return;
        }
        PlayerConfig config = Main.getInstance().getPlayerConfig();
        PlayerSettings settings = getSettings(playerId);
        config.setProfileSpellInputMode(playerId, slot, settings.getSpellInputMode());
    }

    public void loadProfileSettings(UUID playerId, int slot) {
        if (playerId == null || slot < 0) {
            return;
        }
        PlayerConfig config = Main.getInstance().getPlayerConfig();
        PlayerSettings settings = getSettings(playerId);
        SpellInputMode mode = config.getProfileSpellInputMode(playerId, slot);
        settings.setSpellInputMode(mode);
    }
}
