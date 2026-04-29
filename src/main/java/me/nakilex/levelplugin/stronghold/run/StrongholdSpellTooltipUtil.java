package me.nakilex.levelplugin.stronghold.run;

import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.Locale;

public final class StrongholdSpellTooltipUtil {
    private StrongholdSpellTooltipUtil() {
    }

    public static void appendSpellEffectLore(List<String> lore, String spellId) {
        String normalized = normalize(spellId);
        switch (normalized) {
            case "warrior_earthquake", "warrior_earthquake_tremor", "warrior_earthquake_cataclysm" ->
                    appendHighlightedBullet(lore, "Sends a radial shockwave that ripples the ground for ", ChatColor.RED, "AoE melee damage", " and heavy knockback.");
            case "warrior_rupture_cyclone" ->
                    appendHighlightedBullet(lore, "Spins around you with repeated hits and ", ChatColor.GOLD, "sustained close-range pressure", ".");
            case "warrior_execution_arc" ->
                    appendHighlightedBullet(lore, "Casts a sweeping execution arc that ", ChatColor.RED, "bursts nearby enemies", ".");
            case "warrior_guarded_resolve" ->
                    appendHighlightedBullet(lore, "Creates a defensive ward that blocks ", ChatColor.AQUA, "incoming hits", " before breaking.");
            case "mage_fireball_basic", "mage_fireball_barrage", "mage_fireball_inferno", "mage_fireball_chainlightning" ->
                    appendHighlightedBullet(lore, "Launches projectiles with ", ChatColor.RED, "ranged burst damage", ", splash chains, and stronger burn uptime.");
            case "blackhole", "blackhole_gravitywell", "blackhole_singularity" ->
                    appendHighlightedBullet(lore, "Pulls enemies into a control zone that deals ", ChatColor.DARK_PURPLE, "stacking tick damage", ", rotating horizon arcs, and collapse burst.");
            case "meteor", "meteor_double", "meteor_big" ->
                    appendHighlightedBullet(lore, "Calls down a meteor strike for ", ChatColor.RED, "high impact area damage", " with emberfall chain impacts at higher ranks.");
            case "mage_heal", "mage_heal_rejuvenation", "mage_heal_party" ->
                    appendHighlightedBullet(lore, "Restores health with a ", ChatColor.GREEN, "support pulse", " for survivability.");
            case "mage_blink", "mage_blink_phase", "mage_blink_rift" ->
                    appendHighlightedBullet(lore, "Blink is ", ChatColor.AQUA, "mana free", " in Stronghold and uses rank-based charges. Riftstride adds a flame sphere burst on arrival.");
            case "archer_quickshot_basic", "archer_quickshot_seeker", "archer_quickshot_payload" ->
                    appendHighlightedBullet(lore, "Fires precision volleys with ", ChatColor.RED, "rapid ranged hits", ", stronger homing, and payload explosions.");
            case "archer_homing_barrage" ->
                    appendHighlightedBullet(lore, "Unleashes tracking arrows for ", ChatColor.YELLOW, "reliable target pressure", ".");
            case "archer_arrow_rain" ->
                    appendHighlightedBullet(lore, "Bombards a zone with ", ChatColor.RED, "multi-hit area volleys", ".");
            case "archer_windguard" ->
                    appendHighlightedBullet(lore, "Applies a support windguard for ", ChatColor.AQUA, "speed and rotation safety", ".");
            case "rogue_arc_basic", "rogue_arc_basic_tempest", "rogue_arc_basic_reaper" ->
                    appendHighlightedBullet(lore, "Delivers quick melee slashes with ", ChatColor.RED, "steady front-line DPS", ".");
            case "rogue_sky_ripper", "rogue_sky_ripper_tempest", "rogue_sky_ripper_execution" ->
                    appendHighlightedBullet(lore, "Performs chained strikes for ", ChatColor.RED, "combo burst damage", " followed by an empowered aerial slam.");
            case "rogue_phantom_cross", "rogue_phantom_cross_cyclone", "rogue_phantom_cross_judgement" ->
                    appendHighlightedBullet(lore, "Lunges through targets with ", ChatColor.RED, "gap-close burst pressure", ".");
            case "rogue_veil_counter", "rogue_veil_counter_obscure", "rogue_veil_counter_dread" ->
                    appendHighlightedBullet(lore, "Drops a smoke zone for ", ChatColor.DARK_GRAY, "defensive control", " and safer uptime.");
            default ->
                    appendHighlightedBullet(lore, "Adds this spell to your Stronghold loadout for ", ChatColor.WHITE, "auto-cast rotation value", ".");
        }
    }

    public static void appendUpgradeDeltaLore(List<String> lore, String baseSpellId, int currentRank, boolean unlock) {
        String normalized = normalize(baseSpellId);
        if (unlock) {
            appendHighlightedBullet(lore, "Unlocks this spell at ", ChatColor.GREEN, "Rank 1", " and adds it to your active auto-cast set.");
            return;
        }
        int nextRank = Math.max(1, currentRank + 1);
        switch (normalized) {
            case "warrior_earthquake" ->
                    appendHighlightedBullet(lore, "Rank " + currentRank + " → " + nextRank + ": massively improves ", ChatColor.RED, "damage, knockback, and quake radius", ".");
            case "warrior_rupture_cyclone" ->
                    appendHighlightedBullet(lore, "Rank " + currentRank + " → " + nextRank + ": greatly improves ", ChatColor.RED, "pulse count, radius growth, and tick damage", ".");
            case "warrior_execution_arc" ->
                    appendHighlightedBullet(lore, "Rank " + currentRank + " → " + nextRank + ": improves ", ChatColor.RED, "orbital strike damage and control pressure", ".");
            case "warrior_guarded_resolve" ->
                    appendHighlightedBullet(lore, "Rank " + currentRank + " → " + nextRank + ": improves ", ChatColor.AQUA, "blocked hits, duration, and party coverage", ".");
            case "mage_heal" ->
                    appendHighlightedBullet(lore, "Rank " + currentRank + " → " + nextRank + ": improves ", ChatColor.GREEN, "healing, mana restore, and shield strength", ".");
            case "mage_blink" ->
                    appendHighlightedBullet(lore, "Rank " + currentRank + " → " + nextRank + ": grants ", ChatColor.AQUA, "+1 Blink charge", " in Stronghold. Highest rank unlocks a fiery sphere burst on arrival.");
            case "blackhole" ->
                    appendHighlightedBullet(lore, "Rank " + currentRank + " → " + nextRank + ": drastically improves ", ChatColor.DARK_PURPLE, "pull radius, horizon arc pressure, and collapse burst", ".");
            case "meteor" ->
                    appendHighlightedBullet(lore, "Rank " + currentRank + " → " + nextRank + ": drastically improves ", ChatColor.RED, "impact damage, blast radius, and emberfall chain strikes", ".");
            case "archer_windguard" ->
                    appendHighlightedBullet(lore, "Rank " + currentRank + " → " + nextRank + ": improves ", ChatColor.AQUA, "uptime and party safety radius", ".");
            case "archer_arrow_rain", "archer_homing_barrage", "archer_quickshot_basic", "rogue_arc_basic",
                 "rogue_sky_ripper", "rogue_phantom_cross" ->
                    appendHighlightedBullet(lore, "Rank " + currentRank + " → " + nextRank + ": improves ", ChatColor.RED, "damage, effective range, and hit consistency", ".");
            case "rogue_veil_counter" ->
                    appendHighlightedBullet(lore, "Rank " + currentRank + " → " + nextRank + ": improves ", ChatColor.DARK_GRAY, "smoke utility and control effect", ".");
            default ->
                    appendHighlightedBullet(lore, "Rank " + currentRank + " → " + nextRank + ": improves ", ChatColor.YELLOW, "cooldown, damage, or effect value", ".");
        }
    }

    private static void appendHighlightedBullet(List<String> lore,
                                                String prefix,
                                                ChatColor valueColor,
                                                String value,
                                                String suffix) {
        if (lore == null) {
            return;
        }
        String line = ChatColor.GRAY + (prefix == null ? "" : prefix)
                + (valueColor == null ? ChatColor.WHITE : valueColor)
                + (value == null ? "" : value)
                + ChatColor.GRAY
                + (suffix == null ? "" : suffix);
        List<String> wrapped = TooltipUtil.wrapLoreLine(line, 210, ChatColor.DARK_GRAY + "  " + ChatColor.GRAY);
        if (wrapped.isEmpty()) {
            return;
        }
        lore.add(TooltipUtil.bulletLine(wrapped.get(0)));
        for (int i = 1; i < wrapped.size(); i++) {
            lore.add(wrapped.get(i));
        }
    }

    private static String normalize(String spellId) {
        return spellId == null ? "" : spellId.toLowerCase(Locale.ROOT);
    }
}
