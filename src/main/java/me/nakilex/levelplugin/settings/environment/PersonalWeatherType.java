package me.nakilex.levelplugin.settings.environment;

public enum PersonalWeatherType {
    CLEAR,
    RAIN,
    THUNDER,
    RESET;

    public PersonalWeatherType cycle(boolean forward) {
        PersonalWeatherType[] values = values();
        int next = forward ? (ordinal() + 1) % values.length : (ordinal() - 1 + values.length) % values.length;
        return values[next];
    }

    public static PersonalWeatherType fromInput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            return PersonalWeatherType.valueOf(input.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
