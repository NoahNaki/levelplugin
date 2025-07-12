package me.nakilex.levelplugin.quests.data;

import me.nakilex.levelplugin.Main;
import org.bukkit.entity.Player;

/** Interface for quests that must clean up listeners when reset or completed. */
public interface QuestResetScript {
    void onReset(Player player, Main plugin);
}
