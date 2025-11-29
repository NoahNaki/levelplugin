package me.nakilex.levelplugin.dungeon.verified;

import me.nakilex.levelplugin.dungeon.DungeonManager;
import org.bukkit.entity.Player;

public interface VerifiedDungeonDefinition {
    String getKey();
    String getDisplayName();

    default boolean matches(String key) {
        return getKey().equals(DungeonManager.normalizeKey(key));
    }

    void register(DungeonManager manager);

    void startInstance(DungeonManager manager, Player player);
}
