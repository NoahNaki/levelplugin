package me.nakilex.levelplugin.particles;

import java.util.Locale;
import java.util.Optional;

public enum ParticleCenter {
    SELF,
    LOOK;

    public static Optional<ParticleCenter> fromToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "self", "player" -> Optional.of(SELF);
            case "look", "target" -> Optional.of(LOOK);
            default -> Optional.empty();
        };
    }
}
