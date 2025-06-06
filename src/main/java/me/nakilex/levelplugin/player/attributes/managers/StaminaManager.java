package me.nakilex.levelplugin.player.attributes.managers;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Simple stamina tracking using the food bar as a visual indicator.
 */
public class StaminaManager {
    private static final StaminaManager instance = new StaminaManager();
    public static StaminaManager getInstance() { return instance; }

    // Current stamina values for players
    private final Map<UUID, Double> staminaMap = new HashMap<>();

    private static final double BASE_MAX_STAMINA = 100.0;
    private static final double HEALTH_SCALING = 5.0; // per health stat point

    public double getMaxStamina(Player player) {
        int hpStat = StatsManager.getInstance().getStatValue(player, StatsManager.StatType.HP);
        return BASE_MAX_STAMINA + hpStat * HEALTH_SCALING;
    }

    public double getStamina(Player player) {
        return staminaMap.getOrDefault(player.getUniqueId(), getMaxStamina(player));
    }

    public void setStamina(Player player, double value) {
        double max = getMaxStamina(player);
        staminaMap.put(player.getUniqueId(), Math.max(0, Math.min(value, max)));
    }

    public void changeStamina(Player player, double delta) {
        setStamina(player, getStamina(player) + delta);
    }
}
