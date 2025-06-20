package me.nakilex.levelplugin.quests.data;

import me.nakilex.levelplugin.Main;
import org.bukkit.entity.Player;

/**
 * Optional interface for quests that need custom logic when starting.
 */
public interface QuestScript {
    /**
     * Called when the quest is started by a player.
     *
     * @param player the player starting the quest
     * @param plugin plugin instance for scheduling tasks or accessing managers
     */
    void onStart(Player player, Main plugin);
}
