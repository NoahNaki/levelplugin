package me.nakilex.levelplugin.mercenary;

/** Role archetype for a mercenary. Used to apply party synergy bonuses. */
public enum MercenaryRole {
    TANK,
    DPS,
    SUPPORT;

    public static MercenaryRole fromString(String value) {
        if (value == null) {
            return DPS;
        }
        try {
            return MercenaryRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return DPS;
        }
    }
}
