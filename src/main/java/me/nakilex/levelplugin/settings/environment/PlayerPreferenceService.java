package me.nakilex.levelplugin.settings.environment;

import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public class PlayerPreferenceService {

    public void saveWeather(Player player, PersonalWeatherType type) {
        if (player == null || type == null) {
            return;
        }
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (type == PersonalWeatherType.RESET) {
            pdc.remove(PersonalEnvironmentKeys.PLAYER_WEATHER);
            return;
        }
        pdc.set(PersonalEnvironmentKeys.PLAYER_WEATHER, PersistentDataType.STRING, type.name());
    }

    public void saveTime(Player player, PersonalTimeType type) {
        if (player == null || type == null) {
            return;
        }
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (type == PersonalTimeType.RESET) {
            pdc.remove(PersonalEnvironmentKeys.PLAYER_TIME);
            return;
        }
        pdc.set(PersonalEnvironmentKeys.PLAYER_TIME, PersistentDataType.STRING, type.name());
    }

    public Optional<PersonalWeatherType> getWeather(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        String raw = player.getPersistentDataContainer().get(PersonalEnvironmentKeys.PLAYER_WEATHER, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(PersonalWeatherType.valueOf(raw.toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public Optional<PersonalTimeType> getTime(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        String raw = player.getPersistentDataContainer().get(PersonalEnvironmentKeys.PLAYER_TIME, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(PersonalTimeType.valueOf(raw.toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public void clearWeather(Player player) {
        if (player == null) {
            return;
        }
        player.getPersistentDataContainer().remove(PersonalEnvironmentKeys.PLAYER_WEATHER);
    }

    public void clearTime(Player player) {
        if (player == null) {
            return;
        }
        player.getPersistentDataContainer().remove(PersonalEnvironmentKeys.PLAYER_TIME);
    }
}
