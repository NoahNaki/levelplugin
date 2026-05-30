package me.nakilex.levelplugin.npc.dialog.model;

import java.util.Objects;

/** Typed key for values shared across dialogue entries. */
public final class ContextKey<T> {
    private final String id;
    private final Class<T> type;

    public ContextKey(String id, Class<T> type) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
    }

    public String id() { return id; }
    public Class<T> type() { return type; }
}
