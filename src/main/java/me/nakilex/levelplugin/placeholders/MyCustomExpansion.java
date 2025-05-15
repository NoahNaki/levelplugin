package me.nakilex.levelplugin.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class MyCustomExpansion extends PlaceholderExpansion {

    private final Main plugin;
    private final GemsManager gemsManager;

    public MyCustomExpansion(Main plugin) {
        this.plugin = plugin;
        this.gemsManager = plugin.getGemsManager();
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

        if (identifier.equalsIgnoreCase("gems")) {
            int rawGems = plugin.getGemsManager().getTotalUnits(player);
            // Convert int to long if needed, or keep int if your method uses int
            long gems = rawGems;

            // Use your formatting method
            return formatBalance(gems);
        }

        if (identifier.equalsIgnoreCase("level")) {
            LevelManager levelManager = LevelManager.getInstance();
            if (levelManager == null) return "1"; // Default level if LevelManager isn't initialized
            return String.valueOf(levelManager.getLevel(player));
        }


        if (identifier.startsWith("party_member_")) {
            // e.g. identifier = "party_member_2"
            int slot;
            try {
                slot = Integer.parseInt(identifier.substring("party_member_".length()));
            } catch (NumberFormatException e) {
                return "";
            }

            PartyManager partyManager = plugin.getPartyManager();
            Party party = partyManager.getParty(player.getUniqueId());
            if (party == null) {
                return slot == 1 ? "No Party" : "";
            }

            List<UUID> members = party.getMembers();
            if (slot < 1 || slot > members.size()) {
                return "";
            }

            UUID memberId = members.get(slot - 1);
            Player online = Bukkit.getPlayer(memberId);

            if (online != null && online.isOnline()) {
                // online: show level, name, health
                LevelManager lm = LevelManager.getInstance();
                int memberLevel = lm == null ? 1 : lm.getLevel(online);
                int hp = (int) online.getHealth();
                return "&7[" + memberLevel + "]&f " + online.getName() + " &c" + hp + " ❤";
            } else {
                // offline: show name in light gray
                String name = Bukkit.getOfflinePlayer(memberId).getName();
                return "&7" + (name != null ? name : "Unknown");
            }
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
