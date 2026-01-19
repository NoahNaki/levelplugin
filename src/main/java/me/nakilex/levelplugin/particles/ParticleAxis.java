package me.nakilex.levelplugin.particles;

import java.util.Locale;
import java.util.Optional;

public enum ParticleAxis {
    X,
    Y,
    Z,
    LOOK;

    public static Optional<ParticleAxis> fromToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String token = raw.trim().toUpperCase(Locale.ROOT);
        return switch (token) {
            case "X" -> Optional.of(X);
            case "Y" -> Optional.of(Y);
            case "Z" -> Optional.of(Z);
            case "LOOK", "FORWARD", "FACING" -> Optional.of(LOOK);
            default -> Optional.empty();
        };
    }
}
