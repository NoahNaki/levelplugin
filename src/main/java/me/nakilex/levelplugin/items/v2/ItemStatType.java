package me.nakilex.levelplugin.items.v2;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ItemStatType {
    HP,
    DEF,
    STR,
    AGI,
    INTEL,
    DEX,
    WIL,
    TEC;

    private static final Map<String, ItemStatType> BY_KEY = Stream.of(values())
            .collect(Collectors.toMap(type -> type.name().toLowerCase(Locale.ROOT), type -> type));

    public static ItemStatType fromKey(String key) {
        if (key == null) return null;
        String normalized = key.toLowerCase(Locale.ROOT).replace(" ", "");
        return BY_KEY.get(normalized);
    }
}
