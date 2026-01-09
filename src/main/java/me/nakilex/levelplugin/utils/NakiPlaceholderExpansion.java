package me.nakilex.levelplugin.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.player.profile.ProfileManager;
import me.nakilex.levelplugin.player.profile.PlayerProfile;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * PlaceholderAPI expansion providing simple player-related placeholders.
 * The identifier is {@code naki} so placeholders take the form
 * <code>%naki_&lt;key&gt;%</code>.
 */
public class NakiPlaceholderExpansion extends PlaceholderExpansion {
    private final Main plugin;
    private final Map<String, Function<Player, String>> placeholders = new HashMap<>();

    public NakiPlaceholderExpansion(Main plugin) {
        this.plugin = plugin;

        placeholders.put("level", p -> String.valueOf(plugin.getLevelManager().getLevel(p)));
        placeholders.put("class", p -> {
            PlayerClass pc = PlayerClassManager.getInstance().getPlayerClass(p);
            return pc.getDisplayName();
        });
        placeholders.put("coins", p -> NumberUtil.formatCommas(plugin.getEconomyManager().getBalance(p)));
        placeholders.put("gems", p -> NumberUtil.formatCommas(plugin.getGemsManager().getTotalUnits(p)));
        placeholders.put("currentmana", p -> {
            StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(p.getUniqueId());
            return String.valueOf(ps.getCurrentMana());
        });
        placeholders.put("maxmana", p -> {
            StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(p.getUniqueId());
            return String.valueOf(ps.getMaxMana());
        });
        placeholders.put("currentxp", p -> String.valueOf(plugin.getLevelManager().getXP(p)));
        placeholders.put("xpnextlevel", p -> String.valueOf(plugin.getLevelManager().getXpNeededForNextLevel(p)));
        placeholders.put("seasondate", p -> plugin.getCalendarManager().getSeasonDate(false));
        placeholders.put("left_mouse_click", p -> "[papi:rf_lmb]");
        placeholders.put("right_mouse_click", p -> "[papi:rf_rmb]");
    }

    @Override
    public String getIdentifier() {
        return "naki";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null || params == null) return "";
        String key = params.toLowerCase();
        if (key.startsWith("profile")) {
            try {
                int slot = Integer.parseInt(key.substring(7)) - 1;
                PlayerProfile prof = ProfileManager.getInstance().getProfile(player.getUniqueId(), slot);
                return prof != null ? prof.getName() : "";
            } catch (NumberFormatException ignored) {
                return "";
            }
        }
        Function<Player, String> handler = placeholders.get(key);
        return handler != null ? handler.apply(player) : null;
    }
}
