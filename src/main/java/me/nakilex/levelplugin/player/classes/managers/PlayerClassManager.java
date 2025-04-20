package me.nakilex.levelplugin.player.classes.managers;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerClassManager {

    private static PlayerClassManager instance;
    public static PlayerClassManager getInstance() {
        if (instance == null) {
            instance = new PlayerClassManager();
        }
        return instance;
    }

    private final Map<UUID, PlayerClass> classMap = new HashMap<>();

    private PlayerClassManager() {}

    public PlayerClass getPlayerClass(UUID uuid) {
        return classMap.getOrDefault(uuid, PlayerClass.VILLAGER);
    }

    public PlayerClass getPlayerClass(Player player) {
        if (player == null) return PlayerClass.VILLAGER;
        return getPlayerClass(player.getUniqueId());
    }

    public void setPlayerClass(UUID uuid, PlayerClass playerClass) {
        classMap.put(uuid, playerClass);
        // Debug
        Bukkit.getLogger().info("[PlayerClassManager] Set UUID " + uuid + " to " + playerClass.name());
    }

    public void setPlayerClass(Player player, PlayerClass playerClass) {
        if (player == null) return;
        setPlayerClass(player.getUniqueId(), playerClass);
        // Debug log
        Bukkit.getLogger().info("[PlayerClassManager] Set " + player.getName() +
            " (" + player.getUniqueId() + ") to " + playerClass.name());

        // Update flight permission based on the new class.
        // If the new class is ARCHER, allow flight; otherwise, disable it.
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (playerClass == PlayerClass.ARCHER) {
                player.setAllowFlight(true);
            } else {
                player.setAllowFlight(false);
                // Also, if the player was flying, stop them from flying.
                if (player.isFlying()) {
                    player.setFlying(false);
                }
            }
        }
    }

}
