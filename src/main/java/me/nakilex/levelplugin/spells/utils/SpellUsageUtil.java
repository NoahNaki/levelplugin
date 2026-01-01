package me.nakilex.levelplugin.spells.utils;

import me.nakilex.levelplugin.spells.Spell;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class SpellUsageUtil {
    private static final Map<String, String> SPELL_USAGE = new HashMap<>();

    static {
        SPELL_USAGE.put("quick_shot", "Right Click");
        SPELL_USAGE.put("backstep", "Left Click");
        SPELL_USAGE.put("arrow_barrage", "Sneak");
        SPELL_USAGE.put("bow_drone", "Sneak + Left Click");
        SPELL_USAGE.put("dragon_piercer", "Sneak + Right Click");

        SPELL_USAGE.put("blazing_feathers", "Left Click");
        SPELL_USAGE.put("ashdance", "Right Click");
        SPELL_USAGE.put("flameburst_convergence", "Sneak");
        SPELL_USAGE.put("phoenix_totem", "Passive");
        SPELL_USAGE.put("pyroclasmic_barrage", "Sneak + Right Click");
        SPELL_USAGE.put("phoenix_rebirth", "Sneak + Left Click");

        SPELL_USAGE.put("brutal_strike", "Left Click");
        SPELL_USAGE.put("charge", "Right Click");
        SPELL_USAGE.put("chain_hook", "Sneak + Right Click");
        SPELL_USAGE.put("shield_barrier", "Sneak");
        SPELL_USAGE.put("shockwave", "Sneak + Left Click");
        SPELL_USAGE.put("whirlwind", "Sneak + Right Click");
        SPELL_USAGE.put("judgement", "Sneak + Left Click");
        SPELL_USAGE.put("rampage", "Sneak + Left Click (low HP)");

        SPELL_USAGE.put("rageblade", "Left Click");
        SPELL_USAGE.put("primal_axe", "Right Click");
        SPELL_USAGE.put("war_cry", "Sneak");
        SPELL_USAGE.put("double_edge", "Sneak + Right Click");
        SPELL_USAGE.put("relentless_leap", "Sneak + Toggle");
        SPELL_USAGE.put("eternal_fury", "Sneak + Left Click");

        SPELL_USAGE.put("holy_strike", "Left Click");
        SPELL_USAGE.put("bound_seal", "Right Click");
        SPELL_USAGE.put("hammer_of_justice", "Sneak");
        SPELL_USAGE.put("heavenly_shield", "Sneak + Right Click");
        SPELL_USAGE.put("unbreakable_will", "Sneak + Left Click");
        SPELL_USAGE.put("last_stand", "Sneak + Left Click");

        SPELL_USAGE.put("death_strike", "Left Click");
        SPELL_USAGE.put("phantom_charge", "Right Click");
        SPELL_USAGE.put("wraithbound_chains", "Sneak");
        SPELL_USAGE.put("soul_barrier", "Sneak");
        SPELL_USAGE.put("necrotic_whirlwind", "Sneak + Right Click");
        SPELL_USAGE.put("death_sentence", "Sneak + Left Click");
        SPELL_USAGE.put("aqua_slash", "Left Click");
        SPELL_USAGE.put("abyssal_dash", "Right Click");
        SPELL_USAGE.put("tidal_wave", "Sneak + Right Click");
        SPELL_USAGE.put("aqua_aura", "Sneak");
        SPELL_USAGE.put("abyssal_smash", "Sneak + Left Click");
        SPELL_USAGE.put("blade_slash", "Left Click");
        SPELL_USAGE.put("assassin_dash", "Right Click");
        SPELL_USAGE.put("dagger_throw", "Sneak + Right Click");
        SPELL_USAGE.put("blade_dance", "Sneak + Left Click");
        SPELL_USAGE.put("shadow_walk", "Sneak");
        // Awakened Rogue
        SPELL_USAGE.put("lethal_combo", "Left Click");
        SPELL_USAGE.put("ravaging_dash", "Right Click");
        SPELL_USAGE.put("crimson_arc", "Sneak + Right Click");
        SPELL_USAGE.put("last_dance", "Sneak + Left Click");
        SPELL_USAGE.put("shadowquake", "Left + Left + Right");
        SPELL_USAGE.put("death_bloom", "Sneak + Toggle");
        SPELL_USAGE.put("deadly_calm", "Passive");
        // Awakened Warrior
        SPELL_USAGE.put("bulwark_instinct", "Passive");
        SPELL_USAGE.put("brutal_combo", "Left Click");
        SPELL_USAGE.put("berserkers_leap", "Right Click");
        SPELL_USAGE.put("relentless_whirlwind", "Sneak + Right Click");
        SPELL_USAGE.put("bloodbound_barrier", "Sneak");
        SPELL_USAGE.put("vicious_strike", "Sneak + Toggle");
        SPELL_USAGE.put("strike_of_fury", "Sneak + Left Click");
        // Awakened Archer
        SPELL_USAGE.put("blasting_combo", "Left Click");
        SPELL_USAGE.put("evasive_shot", "Left + Right + Left");
        SPELL_USAGE.put("piercing_skyfall", "Left + Left + Left");
        SPELL_USAGE.put("rapid_arrows", "Right + Left + Left");
        SPELL_USAGE.put("shot_of_destruction", "Right + Right + Right");
        SPELL_USAGE.put("volley_of_arrows", "Sneak + Toggle");
        SPELL_USAGE.put("ambush", "Passive");

        // Awakened Mage
        SPELL_USAGE.put("sorcery_combo", "Left Click");
        SPELL_USAGE.put("teleport_strike", "Right Click");
        SPELL_USAGE.put("hailpiercer", "Sneak + Left Click");
        SPELL_USAGE.put("meteor_of_doom", "Sneak + Right Click");
        SPELL_USAGE.put("blazing_barrage", "Sneak (Hold then Release)");
        SPELL_USAGE.put("cryo_prison", "Sneak Twice");
        SPELL_USAGE.put("mana_barrier", "Sneak (Start)");

        SPELL_USAGE.put("fireball", "Left Click");
        SPELL_USAGE.put("blink", "Right Click");
        SPELL_USAGE.put("meteor", "Sneak + Right Click");
        SPELL_USAGE.put("frost_nova", "Sneak");
        SPELL_USAGE.put("inferno_chains", "Sneak + Left Click");

        SPELL_USAGE.put("dragonian_slash", "Left Click");
        SPELL_USAGE.put("dragonian_lunge", "Right Click");
        SPELL_USAGE.put("dragonian_rs", "Sneak + Right Click");
        SPELL_USAGE.put("dragonian_ss", "Sneak");
        SPELL_USAGE.put("taotie_dragon", "Sneak + Left Click");

        SPELL_USAGE.put("gale_slash", "Left Click");
        SPELL_USAGE.put("vault", "Right Click");
        SPELL_USAGE.put("dancing_blade", "Sneak + Right Click");
        SPELL_USAGE.put("torrent", "Sneak + Toggle");
        SPELL_USAGE.put("cloudpiercer", "Sneak");
        SPELL_USAGE.put("windbound_fury", "Sneak + Left Click");

        SPELL_USAGE.put("frost_strike", "Left Click");
        SPELL_USAGE.put("glacial_impalement", "Right Click");
        SPELL_USAGE.put("frozen_shield", "Sneak + Right Click");
        SPELL_USAGE.put("arctic_charge", "Sneak");
        SPELL_USAGE.put("glacier_smash", "Sneak + Toggle");
        SPELL_USAGE.put("permafrost_lance", "Sneak + Left Click");
    }

    private SpellUsageUtil() {
    }

    public static String getUsageLabel(Spell spell) {
        if (spell == null) {
            return "Unknown";
        }
        String id = spell.getId();
        if (id != null) {
            String usage = SPELL_USAGE.get(id.toLowerCase(Locale.ROOT));
            if (usage != null) {
                return usage;
            }
        }
        return comboLabel(spell.getCombo());
    }

    private static String comboLabel(String combo) {
        if (combo == null || combo.isBlank()) {
            return "Unknown";
        }
        return switch (combo) {
            case "LEFT" -> "Left Click";
            case "RIGHT" -> "Right Click";
            case "SHIFT_LEFT" -> "Sneak + Left Click";
            case "SHIFT_RIGHT" -> "Sneak + Right Click";
            case "SNEAK" -> "Sneak";
            case "BASIC_ATTACK" -> "Basic Attack";
            default -> {
                String normalized = combo.trim();
                if (normalized.matches("[LR]+")) {
                    StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < normalized.length(); i++) {
                        String part = normalized.charAt(i) == 'L' ? "Left" : "Right";
                        if (i > 0) {
                            builder.append(" + ");
                        }
                        builder.append(part);
                    }
                    yield builder.toString();
                }
                yield normalized;
            }
        };
    }
}
