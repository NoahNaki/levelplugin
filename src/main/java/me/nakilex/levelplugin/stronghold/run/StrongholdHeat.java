package me.nakilex.levelplugin.stronghold.run;

import me.nakilex.levelplugin.dungeon.modifiers.RunModifier;
import org.bukkit.ChatColor;

import java.util.List;

/** Selectable Stronghold risk/reward profiles backed by the shared RunModifier hooks. */
public enum StrongholdHeat implements RunModifier {
    NONE("none", "No Heat", ChatColor.GRAY,
            List.of("Standard Stronghold pacing."), 1.0, 1.0, 1.0, 0, 0.18),
    GLASS("glass", "Curse of Glass", ChatColor.RED,
            List.of("+25% damage taken.", "+20% score multiplier."), 1.25, 1.0, 1.20, 0, 0.22),
    SWARMS("swarms", "Curse of Swarms", ChatColor.DARK_GREEN,
            List.of("+25% enemies per wave.", "+15% score multiplier."), 1.0, 1.25, 1.15, 0, 0.24),
    ELITES("elites", "Elite Infestation", ChatColor.LIGHT_PURPLE,
            List.of("Elite hunt objectives appear more often.", "+15% score multiplier."), 1.0, 1.0, 1.15, 0, 0.36),
    MAYHEM("mayhem", "Mayhem", ChatColor.GOLD,
            List.of("+20% damage taken.", "+20% enemies per wave.", "+35% score multiplier."), 1.20, 1.20, 1.35, 0, 0.34);

    private static final StrongholdHeat[] VALUES = values();

    private final String id;
    private final String displayName;
    private final ChatColor color;
    private final List<String> description;
    private final double damageTakenMultiplier;
    private final double waveMobMultiplier;
    private final double scoreMultiplier;
    private final int flatWaveMobBonus;
    private final double eliteChance;

    StrongholdHeat(String id, String displayName, ChatColor color, List<String> description,
                   double damageTakenMultiplier, double waveMobMultiplier, double scoreMultiplier,
                   int flatWaveMobBonus, double eliteChance) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.description = description;
        this.damageTakenMultiplier = damageTakenMultiplier;
        this.waveMobMultiplier = waveMobMultiplier;
        this.scoreMultiplier = scoreMultiplier;
        this.flatWaveMobBonus = flatWaveMobBonus;
        this.eliteChance = eliteChance;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return displayName; }
    public ChatColor color() { return color; }
    public List<String> description() { return description; }
    public String coloredName() { return color + displayName; }

    @Override
    public int modifyWaveMobCount(int baseCount) {
        return Math.max(1, (int) Math.round(baseCount * waveMobMultiplier) + flatWaveMobBonus);
    }

    @Override
    public double modifyDamageTaken(double baseDamage) {
        return Math.max(0.0, baseDamage * damageTakenMultiplier);
    }

    @Override
    public double modifyScoreMultiplier(double baseMultiplier) {
        return baseMultiplier * scoreMultiplier;
    }

    @Override
    public double modifyEliteObjectiveChance(double baseChance) {
        return Math.max(baseChance, eliteChance);
    }

    public StrongholdHeat next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public static StrongholdHeat byId(String id) {
        if (id == null || id.isBlank()) return NONE;
        for (StrongholdHeat heat : VALUES) {
            if (heat.id.equalsIgnoreCase(id) || heat.name().equalsIgnoreCase(id)) return heat;
        }
        return NONE;
    }
}
