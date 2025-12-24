package me.nakilex.levelplugin.player.fishing.managers;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Lightweight tracker for Fishing levels. */
public final class FishingSkillManager {
    private static final FishingSkillManager INSTANCE = new FishingSkillManager();
    private final Map<UUID, Integer> levels = new HashMap<>();

    private FishingSkillManager() {
    }

    public static FishingSkillManager getInstance() {
        return INSTANCE;
    }

    public void initializePlayer(Player player) {
        if (player != null) {
            levels.putIfAbsent(player.getUniqueId(), 1);
        }
    }

    public int getLevel(Player player) {
        if (player == null) {
            return 1;
        }
        return levels.getOrDefault(player.getUniqueId(), 1);
    }
}
