// File: me/nakilex/levelplugin/display/DisplayManager.java
package me.nakilex.levelplugin.display;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.scoreboard.ScoreboardManager;
import org.bukkit.entity.Player;

public class DisplayManager {

    private EconomyManager economyManager;
    private PartyManager partyManager;
    private ScoreboardManager scoreboardManager;

    public DisplayManager(EconomyManager economyManager, PartyManager partyManager, ScoreboardManager scoreboardManager) {
        this.economyManager = economyManager;
        this.partyManager = partyManager;
        this.scoreboardManager = scoreboardManager;
    }

    public void updatePlayerDisplay(Player player) {
        int money = economyManager.getBalance(player);
        Party party = partyManager.getParty(player.getUniqueId());
        // Pass the info to ScoreboardManager to update the scoreboard
        scoreboardManager.updateScoreboard(player, money, party);
    }
}
