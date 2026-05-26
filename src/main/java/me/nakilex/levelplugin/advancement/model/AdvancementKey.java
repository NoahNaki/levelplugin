package me.nakilex.levelplugin.advancement.model;

import java.util.Objects;

public record AdvancementKey(String namespace, String value) {
    public AdvancementKey {
        if (namespace == null || namespace.isBlank()) throw new IllegalArgumentException("namespace cannot be blank");
        if (value == null || value.isBlank()) throw new IllegalArgumentException("value cannot be blank");
    }

    public static AdvancementKey parse(String raw) {
        Objects.requireNonNull(raw, "raw");
        String[] split = raw.split(":", 2);
        if (split.length != 2) throw new IllegalArgumentException("Expected <namespace>:<key>");
        return new AdvancementKey(split[0], split[1]);
    }

    @Override
    public String toString() { return namespace + ":" + value; }
}
