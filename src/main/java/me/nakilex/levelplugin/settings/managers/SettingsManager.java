package me.nakilex.levelplugin.settings.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.player.profile.ProfileManager;
import me.nakilex.levelplugin.settings.data.PlayerSettings;
import me.nakilex.levelplugin.spells.input.SpellInputMode;
import org.bukkit.Bukkit;
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
        SpellInputMode mode = settings.getSpellInputMode();
        config.setProfileSpellInputMode(playerId, slot, mode);
        config.setProfileNpcSoundEffects(playerId, slot, settings.isNpcSoundEffectsEnabled());
        config.setProfileAchievementSoundEffects(playerId, slot, settings.isAchievementSoundEffectsEnabled());
        config.saveConfigFile();
        Bukkit.getLogger().info("[LevelPlugin][SettingsManager] Saved settings for player="
                + playerId + " slot=" + slot + " mode=" + mode
                + " npcSoundEffects=" + settings.isNpcSoundEffectsEnabled()
                + " achievementSoundEffects=" + settings.isAchievementSoundEffectsEnabled());
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            me.nakilex.levelplugin.spells.input.SpellInputHudManager.getInstance().sync(player);
        }
    }

    public void loadProfileSettings(UUID playerId, int slot) {
        if (playerId == null || slot < 0) {
            return;
        }
        PlayerConfig config = Main.getInstance().getPlayerConfig();
        PlayerSettings settings = getSettings(playerId);
        SpellInputMode mode = config.getProfileSpellInputMode(playerId, slot);
        settings.setSpellInputMode(mode);
        settings.setNpcSoundEffects(config.getProfileNpcSoundEffects(playerId, slot));
        settings.setAchievementSoundEffects(config.getProfileAchievementSoundEffects(playerId, slot));
        Bukkit.getLogger().info("[LevelPlugin][SettingsManager] Loaded settings for player="
                + playerId + " slot=" + slot + " mode=" + mode
                + " npcSoundEffects=" + settings.isNpcSoundEffectsEnabled()
                + " achievementSoundEffects=" + settings.isAchievementSoundEffectsEnabled());
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            me.nakilex.levelplugin.spells.input.SpellInputHudManager.getInstance().sync(player);
        }
    }
}
