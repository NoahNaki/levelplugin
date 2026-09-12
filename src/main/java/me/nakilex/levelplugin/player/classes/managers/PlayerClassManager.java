package me.nakilex.levelplugin.player.classes.managers;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import me.nakilex.levelplugin.utils.FlightUtil;

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
        if (!ClassUtil.isClassSystemEnabled()) {
            return PlayerClass.VILLAGER;
        }
        if (uuid == null) {
            return PlayerClass.VILLAGER;
        }
        PlayerClass fromStats = StatsManager.getInstance().getPlayerStats(uuid).playerClass;
        if (fromStats == null) {
            fromStats = PlayerClass.VILLAGER;
        }
        classMap.put(uuid, fromStats);
        return fromStats;
    }

    public PlayerClass getPlayerClass(Player player) {
        if (player == null) return PlayerClass.VILLAGER;
        return getPlayerClass(player.getUniqueId());
    }

    public void setPlayerClass(UUID uuid, PlayerClass playerClass) {
        if (!ClassUtil.isClassSystemEnabled()) {
            return;
        }
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

        // Double-jump only owns allowFlight when it had to enable it itself.
        // Never revoke X-Prison Fly (or another already-active flight source)
        // simply because the selected class has no air jumps.
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            boolean canDoubleJump = playerClass == PlayerClass.ARCHER
                    || playerClass == PlayerClass.ROGUE
                    || playerClass == PlayerClass.DEADEYE
                    || playerClass == PlayerClass.PHOENIXHUNTER;
            if (canDoubleJump) {
                FlightUtil.ensureDoubleJumpFlight(player);
            } else {
                FlightUtil.releaseDoubleJumpFlight(player);
            }
        }
    }

}
