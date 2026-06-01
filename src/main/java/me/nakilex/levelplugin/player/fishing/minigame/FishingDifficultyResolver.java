package me.nakilex.levelplugin.player.fishing.minigame;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.tools.ToolTier;
import me.nakilex.levelplugin.player.fishing.data.FishDefinition;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

/** Resolves one reusable profile from fishing level, hooked fish rarity, and rod tier. */
public final class FishingDifficultyResolver {
    private static final String ROOT = "fishing-mini-games.difficulty";

    private FishingDifficultyResolver() { }

    public static FishingDifficultyProfile resolve(FileConfiguration config, int fishingLevel,
                                                    FishDefinition hookedFish, ToolTier rodTier) {
        if (config == null || !config.getBoolean(ROOT + ".enabled", false) || hookedFish == null) {
            return FishingDifficultyProfile.normal();
        }
        FishingMiniGameDifficulty tier = levelTier(config, fishingLevel)
                .shift(rarityModifier(config, hookedFish.rarity()));
        FishingDifficultyProfile profile = FishingDifficultyProfile.forTier(tier);
        if (rodTier == null) return profile;

        double rodStrength = rodTier.ordinal() / (double) Math.max(1, ToolTier.values().length - 1);
        return profile.withRodAssistance(0.15 * rodStrength, 0.15 * rodStrength, 0.10 * rodStrength);
    }

    private static FishingMiniGameDifficulty levelTier(FileConfiguration config, int fishingLevel) {
        int easyMax = config.getInt(ROOT + ".level-tiers.easy-max-level", 15);
        int normalMax = Math.max(easyMax, config.getInt(ROOT + ".level-tiers.normal-max-level", 40));
        int hardMax = Math.max(normalMax, config.getInt(ROOT + ".level-tiers.hard-max-level", 70));
        if (fishingLevel <= easyMax) return FishingMiniGameDifficulty.EASY;
        if (fishingLevel <= normalMax) return FishingMiniGameDifficulty.NORMAL;
        if (fishingLevel <= hardMax) return FishingMiniGameDifficulty.HARD;
        return FishingMiniGameDifficulty.EXTREME;
    }

    private static int rarityModifier(FileConfiguration config, ItemRarity rarity) {
        ItemRarity safeRarity = rarity == null ? ItemRarity.COMMON : rarity;
        int fallback = switch (safeRarity) {
            case COMMON -> -1;
            case UNCOMMON, RARE -> 0;
            case EPIC -> 1;
            case LEGENDARY, MYTHIC, FABLED -> 2;
        };
        return config.getInt(ROOT + ".rarity-modifiers." + safeRarity.name().toLowerCase(Locale.ROOT), fallback);
    }
}
