package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

/** Shared Nexo spell icon resolver for spell upgrade, deck, and summon UIs. */
public final class SpellIconUtil {
    private SpellIconUtil() {
    }

    public static ItemStack createSpellIcon(String spellId, String displayName) {
        return createSpellIcon(spellId, displayName, 1);
    }

    public static ItemStack createSpellIcon(String spellId, String displayName, int rank) {
        String safeName = displayName == null || displayName.isBlank() ? "Spell" : displayName;
        String iconBaseId = resolveSpellIconBaseId(spellId);
        for (String iconId : resolveTieredIconCandidates(iconBaseId, rank)) {
            ItemStack candidate = GuiUtil.getNexoItem(iconId, ChatColor.GOLD + safeName);
            if (candidate.getType() != Material.BARRIER) {
                return candidate;
            }
        }
        return GuiUtil.createGuiItem(Material.ENCHANTED_BOOK, ChatColor.GOLD + safeName, List.of());
    }

    public static String resolveSpellIconBaseId(String spellId) {
        String normalized = spellId == null ? "" : spellId.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("mage_fireball")) return "flame";
        if (normalized.startsWith("meteor")) return "fire_aspect";
        if (normalized.startsWith("blackhole")) return "curse_of_binding";
        if (normalized.startsWith("mage_heal")) return "mending";
        if (normalized.startsWith("mage_blink")) return "swift_sneak";
        if (normalized.startsWith("archer_quickshot")) return "power";
        if (normalized.startsWith("archer_homing_barrage")) return "multishot";
        if (normalized.startsWith("archer_arrow_rain")) return "piercing";
        if (normalized.startsWith("archer_windguard")) return "feather_falling";
        if (normalized.startsWith("archer_skybound")) return "wind_burst";
        if (normalized.startsWith("rogue_arc_basic")) return "sweeping_edge";
        if (normalized.startsWith("rogue_sky_ripper")) return "sharpness";
        if (normalized.startsWith("rogue_phantom_cross")) return "knockback";
        if (normalized.startsWith("rogue_veil_counter")) return "curse_of_vanishing";
        if (normalized.startsWith("rogue_razor_dash")) return "quick_charge";
        if (normalized.startsWith("warrior_earthquake")) return "density";
        if (normalized.startsWith("warrior_rupture_cyclone")) return "breach";
        if (normalized.startsWith("warrior_titan_vault")) return "blast_protection";
        if (normalized.startsWith("warrior_execution_arc")) return "smite";
        if (normalized.startsWith("warrior_guarded_resolve")) return "protection";
        return "efficiency";
    }

    private static List<String> resolveTieredIconCandidates(String baseIconId, int rank) {
        if (baseIconId == null || baseIconId.isBlank()) {
            return List.of("efficiency");
        }
        int safeRank = Math.max(1, rank);
        if (safeRank <= 1) {
            return List.of(baseIconId, "efficiency");
        }
        return List.of(baseIconId + "_" + safeRank, baseIconId, "efficiency");
    }
}
