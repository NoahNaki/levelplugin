package me.nakilex.levelplugin.particles;

import java.util.Locale;
import java.util.Optional;

public enum ParticleRotationAxis {
    X,
    Y,
    Z;

    public static Optional<ParticleRotationAxis> fromToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String token = raw.trim().toUpperCase(Locale.ROOT);
        return switch (token) {
            case "X" -> Optional.of(X);
            case "Y" -> Optional.of(Y);
            case "Z" -> Optional.of(Z);
            default -> Optional.empty();
        };
    }
}
