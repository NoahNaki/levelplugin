package me.nakilex.levelplugin.player.classes.managers;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
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
        if (uuid == null) {
            return PlayerClass.VILLAGER;
        }
        PlayerClass cached = classMap.get(uuid);
        if (cached != null) {
            return cached;
        }
        PlayerClass fromStats = StatsManager.getInstance().getPlayerStats(uuid).playerClass;
        classMap.put(uuid, fromStats);
        return fromStats;
    }

    public PlayerClass getPlayerClass(Player player) {
        if (player == null) return PlayerClass.VILLAGER;
        return getPlayerClass(player.getUniqueId());
    }

    public void setPlayerClass(UUID uuid, PlayerClass playerClass) {
        if (uuid == null) {
            return;
        }
        if (playerClass == null) {
            playerClass = PlayerClass.VILLAGER;
        }
        StatsManager.getInstance().getPlayerStats(uuid).playerClass = playerClass;
        classMap.put(uuid, playerClass);
    }

    public void setPlayerClass(Player player, PlayerClass playerClass) {
        if (player == null) return;
        setPlayerClass(player.getUniqueId(), playerClass);

        // Update flight permission based on the new class.
        // Archers, Rogues, Deadeye and PhoenixHunter can double jump (flight). Other classes cannot.
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (playerClass == PlayerClass.ARCHER
                    || playerClass == PlayerClass.ROGUE
                    || playerClass == PlayerClass.DEADEYE
                    || playerClass == PlayerClass.PHOENIXHUNTER) {
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
