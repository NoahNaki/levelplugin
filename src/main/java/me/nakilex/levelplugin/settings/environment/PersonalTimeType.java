package me.nakilex.levelplugin.settings.environment;

public enum PersonalTimeType {
    DAY,
    NIGHT,
    SUNSET,
    RESET;

    public PersonalTimeType cycle(boolean forward) {
        PersonalTimeType[] values = values();
        int next = forward ? (ordinal() + 1) % values.length : (ordinal() - 1 + values.length) % values.length;
        return values[next];
    }

    public static PersonalTimeType fromInput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            return PersonalTimeType.valueOf(input.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
