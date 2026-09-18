package me.nakilex.playerspoofer;

enum BotOrigin {
    MANUAL_SINGLE,
    MANUAL_MULTI,
    TARGET;

    static BotOrigin fromStored(String value, boolean legacyBulk) {
        if (value != null) {
            try {
                return BotOrigin.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return legacyBulk ? MANUAL_MULTI : MANUAL_SINGLE;
    }
}
