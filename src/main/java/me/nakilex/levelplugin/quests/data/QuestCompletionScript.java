package me.nakilex.levelplugin.quests.data;

import me.nakilex.levelplugin.Main;
import org.bukkit.entity.Player;

/** Interface for quests that run custom logic when completed. */
public interface QuestCompletionScript {
    void onComplete(Player player, Main plugin);
}
