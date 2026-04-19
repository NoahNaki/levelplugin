package me.nakilex.levelplugin.settings.environment;

import org.bukkit.WeatherType;
import org.bukkit.entity.Player;

import java.util.Optional;

public class PlayerEnvironmentService {

    private final PlayerPreferenceService preferences;

    public PlayerEnvironmentService(PlayerPreferenceService preferences) {
        this.preferences = preferences;
    }

    public PlayerPreferenceService preferences() {
        return preferences;
    }

    public void applyWeather(Player player, PersonalWeatherType type) {
        if (player == null || type == null) {
            return;
        }
        applyWeatherWithoutResave(player, type);
        if (type == PersonalWeatherType.RESET) {
            preferences.clearWeather(player);
        } else {
            preferences.saveWeather(player, type);
        }
    }

    public void applyTime(Player player, PersonalTimeType type) {
        if (player == null || type == null) {
            return;
        }
        applyTimeWithoutResave(player, type);
        if (type == PersonalTimeType.RESET) {
            preferences.clearTime(player);
        } else {
            preferences.saveTime(player, type);
        }
    }

    public void restorePreferences(Player player) {
        if (player == null) {
            return;
        }
        Optional<PersonalWeatherType> weather = preferences.getWeather(player);
        Optional<PersonalTimeType> time = preferences.getTime(player);
        weather.ifPresent(type -> applyWeatherWithoutResave(player, type));
        time.ifPresent(type -> applyTimeWithoutResave(player, type));
    }

    public PersonalWeatherType getCurrentWeatherOrReset(Player player) {
        return preferences.getWeather(player).orElse(PersonalWeatherType.RESET);
    }

    public PersonalTimeType getCurrentTimeOrReset(Player player) {
        return preferences.getTime(player).orElse(PersonalTimeType.RESET);
    }

    private void applyWeatherWithoutResave(Player player, PersonalWeatherType type) {
        switch (type) {
            case CLEAR -> player.setPlayerWeather(WeatherType.CLEAR);
            case RAIN, THUNDER -> player.setPlayerWeather(WeatherType.DOWNFALL);
            case RESET -> player.resetPlayerWeather();
        }
    }

    private void applyTimeWithoutResave(Player player, PersonalTimeType type) {
        switch (type) {
            case DAY -> player.setPlayerTime(1000L, false);
            case NIGHT -> player.setPlayerTime(13000L, false);
            case SUNSET -> player.setPlayerTime(12000L, false);
            case RESET -> player.resetPlayerTime();
        }
    }
}
