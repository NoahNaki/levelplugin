package me.nakilex.levelplugin.player.battlepass;

import me.nakilex.levelplugin.player.battlepass.data.BattlePassView;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Abstraction used by {@link me.nakilex.levelplugin.player.battlepass.gui.BattlePassGUI}
 * to fetch data and perform actions.  Implementations can bridge to existing
 * battle pass logic without the GUI needing to know the specifics.
 */
public interface BattlePassProvider {

    /**
     * Build the current snapshot for the given player.
     */
    BattlePassView view(UUID playerId);

    /**
     * Attempt to claim a reward for the player.
     */
    void claimReward(Player player, int tier, boolean premiumReward);

    /**
     * Called when the player presses the back button in the GUI.
     */
    void handleBack(Player player);

    /**
     * Called when the player opens the received items menu.
     */
    void openReceivedItems(Player player);
}
