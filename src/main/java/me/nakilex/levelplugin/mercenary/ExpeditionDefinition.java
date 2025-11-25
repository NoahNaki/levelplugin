package me.nakilex.levelplugin.mercenary;

/** Simple value object describing a dungeon expedition option. */
public record ExpeditionDefinition(String id, String displayName, int threat,
                                  int baseDurationSeconds, int recommendedGearScore) {
}
