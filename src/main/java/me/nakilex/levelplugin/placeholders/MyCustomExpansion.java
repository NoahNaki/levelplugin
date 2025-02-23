package me.nakilex.levelplugin.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class MyCustomExpansion extends PlaceholderExpansion {

    private final Main plugin;

    public MyCustomExpansion(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Make sure the expansion is not unregistered on reload
     */
    @Override
    public boolean persist() {
        return true;
    }

    /**
     * Return true for registering the expansion
     */
    @Override
    public boolean canRegister() {
        return true;
    }

    /**
     * The identifier is what goes after the % in your placeholders.
     * e.g. %mycustomexp_balance%
     */
    @Override
    public String getIdentifier() {
        return "mycustomexp";
    }

    /**
     * The author name
     */
    @Override
    public String getAuthor() {
        return "Nakilex"; // Update with your actual name
    }

    /**
     * The version of the expansion
     */
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * This is where you match placeholders to the actual data you want to return.
     */
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            // If the placeholder is player-specific, return empty if the player is null
            return "";
        }

        // Example 1: Return the player's display name
        if (identifier.equalsIgnoreCase("displayname")) {
            return player.getDisplayName();
        }

        // Example 2: Suppose your plugin has an economy manager
        // returning a custom currency from your plugin
        if (identifier.equalsIgnoreCase("balance")) {
            int rawBalance = plugin.getEconomyManager().getBalance(player);
            // Convert int to long if needed, or keep int if your method uses int
            long balance = rawBalance;

            // Use your formatting method
            return formatBalance(balance);
        }

        if (identifier.equalsIgnoreCase("level")) {
            LevelManager levelManager = LevelManager.getInstance();
            if (levelManager == null) return "1"; // Default level if LevelManager isn't initialized
            return String.valueOf(levelManager.getLevel(player));
        }


        if (identifier.startsWith("party_member_")) {
            String slotString = identifier.substring("party_member_".length()); // e.g. "1"
            int slot;
            try {
                slot = Integer.parseInt(slotString);
            } catch (NumberFormatException e) {
                return ""; // Invalid number
            }

            PartyManager partyManager = plugin.getPartyManager();
            Party party = partyManager.getParty(player.getUniqueId());
            if (party == null) {
                // If not in a party, show "No Party" for slot 1, blank for others
                return slot == 1 ? "No Party" : "";
            }

            List<UUID> members = party.getMembers();
            if (slot < 1 || slot > members.size()) {
                return ""; // No member at this slot
            }

            UUID memberId = members.get(slot - 1);
            Player member = plugin.getServer().getPlayer(memberId);
            if (member == null || !member.isOnline()) {
                return "";
            }

            // Retrieve the member's level using LevelManager
            LevelManager levelManager = LevelManager.getInstance();
            int memberLevel = (levelManager != null) ? levelManager.getLevel(member) : 1;
            double hp = member.getHealth();

            // Return string with level prefix "[Lv. X] "
            return "&7["+memberLevel+ "]&f " + member.getName() + " &c" + (int) hp + " ❤";
        }
        return null;
    }

    private String formatBalance(long amount) {
        // For negative numbers, handle as you see fit
        if (amount < 0) {
            return "-" + formatBalance(-amount);
        }

        if (amount >= 1_000_000_000) {
            // Billions
            double val = amount / 1_000_000_000.0;
            return String.format("%.2fB", val); // e.g. 1.23B
        } else if (amount >= 1_000_000) {
            // Millions
            double val = amount / 1_000_000.0;
            return String.format("%.2fM", val); // e.g. 2.50M
        } else if (amount >= 1_000) {
            // Thousands
            double val = amount / 1_000.0;
            return String.format("%.2fK", val); // e.g. 999.99K
        } else {
            // Less than 1000
            return String.valueOf(amount);
        }
    }

}
