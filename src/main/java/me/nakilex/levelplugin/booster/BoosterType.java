package me.nakilex.levelplugin.booster;

import org.bukkit.ChatColor;
import org.bukkit.Material;

/** Types of global boosters supported by the plugin. */
public enum BoosterType {
    COIN(ChatColor.GOLD + "Coin Booster", Material.GOLD_INGOT, ChatColor.GOLD),
    COMBAT_XP(ChatColor.DARK_GREEN + "Combat XP Booster", Material.EXPERIENCE_BOTTLE, ChatColor.DARK_GREEN);

    private final String displayName;
    private final Material icon;
    private final ChatColor accent;

    BoosterType(String displayName, Material icon, ChatColor accent) {
        this.displayName = displayName;
        this.icon = icon;
        this.accent = accent;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public ChatColor accent() {
        return accent;
    }

    public String key() {
        return name().toLowerCase();
    }
}
