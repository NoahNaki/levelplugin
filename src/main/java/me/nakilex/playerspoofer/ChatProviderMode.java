package me.nakilex.playerspoofer;

import java.util.Locale;

enum ChatProviderMode {
    TEMPLATE,
    OPENROUTER,
    OLLAMA,
    HYBRID;

    static ChatProviderMode fromConfig(String raw) {
        if (raw == null) return HYBRID;
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return HYBRID;
        }
    }
}
