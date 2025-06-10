package me.nakilex.levelplugin.environment;

import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.environment.stage.TownStageManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles per-player settlement levels and upgrades.
 */
public class EnvironmentManager {
    public static final int MAX_LEVEL = 3;
    private static final int STAGES_PER_LEVEL = 3;
    private final PlayerConfig playerConfig;
    private final TownStageManager stageManager;
    private final Map<UUID, EnvironmentState> states = new HashMap<>();

    public static class EnvironmentState {
        public int level;
        public int stage;
        public int invested;
        public EnvironmentState(int level, int stage) {
            this.level = level;
            this.stage = stage;
        }
    }

    public EnvironmentManager(PlayerConfig config, TownStageManager stageManager) {
        this.playerConfig = config;
        this.stageManager = stageManager;
    }

    /** Load state for player if not present. */
    public void initializePlayer(UUID uuid) {
        states.computeIfAbsent(uuid, id -> {
            int lvl = playerConfig.getEnvironmentLevel(id);
            int stg = playerConfig.getEnvironmentStage(id);
            if (lvl <= 0) lvl = 1;
            if (stg <= 0) stg = 1;
            EnvironmentState es = new EnvironmentState(lvl, stg);
            stageManager.spawnForStage(es.level, es.stage);
            return es;
        });
    }

    public EnvironmentState getState(UUID uuid) {
        return states.get(uuid);
    }

    public void saveState(UUID uuid) {
        EnvironmentState s = states.get(uuid);
        if (s != null) {
            playerConfig.setEnvironmentState(uuid, s.level, s.stage);
            playerConfig.saveConfigFile();
        }
    }

    public void saveAll() {
        for (UUID id : states.keySet()) {
            saveState(id);
        }
    }

    /**
     * Invest materials towards the next upgrade. Currently costs 1 oak log.
     */
    public void invest(Player player, int amount) {
        initializePlayer(player.getUniqueId());
        EnvironmentState state = states.get(player.getUniqueId());
        state.invested += amount;
        if (state.invested >= 1) {
            state.invested = 0;
            advance(state);
            player.sendMessage(ChatColor.GREEN + "Settlement upgraded to Level "
                    + state.level + " Stage " + state.stage + "!");
            stageManager.spawnForStage(state.level, state.stage);
        } else {
            player.sendMessage(ChatColor.GREEN + "Invested " + amount + " oak log.");
        }
    }

    private void advance(EnvironmentState state) {
        state.stage++;
        if (state.stage > STAGES_PER_LEVEL) {
            state.stage = 1;
            if (state.level < MAX_LEVEL) {
                state.level++;
            } else {
                state.stage = STAGES_PER_LEVEL;
            }
        }
    }
}
